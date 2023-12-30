/*******************************************************************************
 * Copyright (c) 2014, 2023  IBM Corporation, Carnegie Mellon University and others
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/

package hulo.model.utils;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import hulo.floormaps.utils.FloormapsArchiver;
import hulo.localization.servlet.MongoService;

public class ExportUtils {

	public static synchronized void export(MongoService mDS, String format, HttpServletRequest request,
			HttpServletResponse response) throws IOException, OutOfMemoryError {
		memSize("start-export: ");
		String filename = request.getParameter("filename");
		if (filename == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no filename");
			return;
		}
		if ("floormaps.json".equals(format)) {
			try {
				FloormapsArchiver.export(filename, request, response);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return;
		}
		List<ObjectId> refids = new ArrayList<>();
		for (String refid : request.getParameterValues("refid")) {
			ObjectId oid = new ObjectId(refid);
			if (!refids.contains(oid)) {
				refids.add(oid);
			}
		}
		String refpoint_id = request.getParameter("refpoint");
		if (refids.size() == 0 || refpoint_id == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no refpoint: " + refpoint_id);
			return;
		}
		Map<String, DBObject> anchorMap = getAnchorMap(mDS);
		Map<String, DBObject> refpointsMap = getRefpointsMap(mDS);
		// System.out.println("floorplanMap=" + anchorMap);
		// System.out.println("refpointMap=" + refpointsMap);
		DBObject refpoint = refpointsMap.get(refpoint_id);
		if (refpoint == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no refpoint: " + refpoint_id);
			return;
		}
		Point2D refpoint_xy = toPoint(refpoint, "x", "y");
		// System.out.println("refids=" + refids + ", refpoint=" + refpoint + " xy=" +
		// refpoint_xy);

		DBObject anchor2 = anchorMap.get(getString(refpoint, "refid"));
		// System.out.println("anchor2=" + anchor2);
		if (anchor2 == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no floorplan: " + refpoint.toString());
			return;
		}
		boolean withUUID = "true".equals(request.getParameter("uuid"));
		String modelParam = request.getParameter("model");
		if (modelParam == null && "Compose".equals(format)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no model data");
		}

		Point2D anchor2_point = toPoint(anchor2, "lat", "lng");
		double anchor2_rotate = getDouble(anchor2, "rotate");

		DBObject query = new BasicDBObject("information.refid", new BasicDBObject("$in", refids));
		DBCursor cursor = mDS.getCollection("samplings").find(query);
		response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
		ArrayNWriter writer = null;
		List<byte[]> samples_data = null, samples_raw_data = null;
		switch (format) {
		case "GeoJSON":
			writer = new ArrayNWriter("{\"features\":[", "],\"type\":\"FeatureCollection\"}", ",", request, response);
			break;
		case "CSV":
			writer = new ArrayNWriter("", "", "\n", request, response);
			break;
		case "JSON":
			writer = new ArrayNWriter("[", "]", ",", request, response);
			break;
		case "Compose":
			samples_data = new ArrayList<byte[]>();
			samples_raw_data = new ArrayList<byte[]>();
			break;
		}
		while (cursor.hasNext()) {
			DBObject sp = cursor.next();
			try {
				DBObject sp_info = (DBObject) sp.get("information");
				DBObject refpoint1 = refpointsMap.get(getString(sp_info, "refid"));
				if (refpoint1 == null) {
					System.err.println("no refpoint: " + sp_info.toString());
					continue;
				}
				DBObject anchor1 = anchorMap.get(getString(refpoint1, "refid"));
				if (anchor1 == null) {
					System.err.println("no floorplan: " + refpoint1.toString());
					continue;
				}
				Point2D local = toPoint(sp_info, "absx", "absy");
				Point2D anchor1_point = toPoint(anchor1, "lat", "lng");
				double anchor1_rotate = getDouble(anchor1, "rotate");
				Point2D global = GeoUtils.xy2latlon(local, anchor1_point, anchor1_rotate);
				Point2D local2 = GeoUtils.latlon2xy(global, anchor2_point, anchor2_rotate);
				Point2D global2 = GeoUtils.xy2latlon(local2, anchor2_point, anchor2_rotate);

				sp_info.put("x", local2.getX());
				sp_info.put("y", local2.getY());
				sp_info.put("absx", local2.getX() + refpoint_xy.getX());
				sp_info.put("absy", local2.getY() + refpoint_xy.getY());
				sp_info.put("lat", global2.getX());
				sp_info.put("lng", global2.getY());

				if (writer == null) {
					(sp.containsField("beacons") ? samples_data : samples_raw_data)
							.add(sp.toString().getBytes("UTF-8"));
					continue;
				}

				switch (format) {
				case "GeoJSON":
					writer.write(samplingToFeature(sp));
					break;
				case "CSV":
					for (Object obj : samplingToCSV(sp, withUUID)) {
						writer.write(obj);
					}
					break;
				case "JSON":
					writer.write(JSON.parse(sp.toString()));
					break;
				}
			} catch (Exception e) {
				System.err.println(sp.toString());
				e.printStackTrace();
			}
		}
		memSize("end-sample: ");
		if (writer != null) {
			writer.close();
			return;
		}

		try {
			JSONObject model = ((JSONObject) JSON.parse(modelParam));
			if (filename.endsWith(".zip")) {
				ModelArchiver.write(model, samples_data, samples_raw_data, response.getOutputStream(), withUUID);
				return;
			}
			writer = new ArrayNWriter("{", "}", null, request, response);
			model.put("samples", new JSONArray());
			model.put("samples_raw", new JSONArray());
			for (Iterator<String> it = model.keys(); it.hasNext();) {
				String key = it.next();
				writer.write("\"" + key + "\":");
				if ("samples".equals(key)) {
					writer.write("[{\"data\":\"");
					boolean separate = false;
					for (byte[] bytes : samples_data) {
						String sample = new String(bytes, "UTF-8");
						for (Object obj : ExportUtils.samplingToCSV((JSONObject) JSON.parse(sample), withUUID)) {
							if (separate) {
								writer.write("\\n");
							}
							writer.write(obj.toString());
							separate = true;
						}
					}
					writer.write("\"}]");
				} else if ("samples_raw".equals(key)) {
					writer.write("[");
					for (int i = 0; i < samples_raw_data.size(); i++) {
						if (i > 0) {
							writer.write(",");
						}
						writer.write(JSON.parse(new String(samples_raw_data.get(i), "UTF-8")));
					}
					writer.write("]");
				} else {
					writer.write(model.get(key));
				}
				if (it.hasNext()) {
					writer.write(",");
				}
			}
			writer.close();
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
			e.printStackTrace();
		} finally {
			memSize("end-export: ");
		}
	}

	private static String getString(DBObject obj, String key) {
		return obj.get(key).toString();
	}

	private static double getDouble(DBObject obj, String key) {
		return ((Number) obj.get(key)).doubleValue();
	}

	private static Point2D toPoint(DBObject obj, String keyX, String keyY) {
		return new Point2D.Double(getDouble(obj, keyX), getDouble(obj, keyY));
	}

	private static Map<String, DBObject> getAnchorMap(MongoService mDS) {
		DBObject keys = new BasicDBObject("lat", 1).append("lng", 1).append("rotate", 1);
		DBCollection floorplans = mDS.getCollection("floorplans");
		DBCursor cursor = floorplans.find(null, keys);
		Map<String, DBObject> map = new HashMap<>();
		while (cursor.hasNext()) {
			DBObject fp = cursor.next();
			map.put(fp.removeField("_id").toString(), fp);
		}
		return map;
	}

	private static Map<String, DBObject> getRefpointsMap(MongoService mDS) {
		DBObject keys = new BasicDBObject("refid", 1).append("x", 1).append("y", 1);
		DBCollection refpoints = mDS.getCollection("refpoints");
		DBCursor cursor = refpoints.find(null, keys);
		Map<String, DBObject> map = new HashMap<>();
		while (cursor.hasNext()) {
			DBObject fp = cursor.next();
			map.put(fp.removeField("_id").toString(), fp);
		}
		return map;
	}

	private static JSONArray samplingToCSV(DBObject sp, boolean withUUID) throws JSONException {
		JSONArray array = new JSONArray();
		if (sp.containsField("beacons")) {
			DBObject info = (DBObject) sp.get("information");
			for (Object _b : (BasicDBList) sp.get("beacons")) {
				DBObject b = (DBObject) _b;
				JSONArray beacon = new JSONArray();
				beacon.put(b.get("timestamp")).put("Beacon").put(info.get("absx")).put(info.get("absy"))
						.put(info.containsField("z") ? info.get("z") : 0).put(info.get("floor_num"));
				int count = 0;
				String uuid = (String)b.get("uuid");
				for (Object _d : (BasicDBList) b.get("data")) {
					DBObject d = (DBObject) _d;
					Number rssi = (Number) d.get("rssi");
					if (rssi.intValue() < 0) {
						if (withUUID) {
							beacon.put(uuid + "-" + d.get("major") + "-" + d.get("minor"));
						} else {
							beacon.put(d.get("major"));
							beacon.put(d.get("minor"));
						}
						beacon.put(rssi);
						count++;
					}
				}
				beacon.put(6, count);
				array.add(beacon.join(","));
			}
		}
		return array;
	}

	public static JSONArray samplingToCSV(JSONObject sp, boolean withUUID) throws JSONException {
		JSONArray array = new JSONArray();
		if (sp.has("beacons")) {
			JSONObject info = sp.getJSONObject("information");
			for (Object _b : sp.getJSONArray("beacons")) {
				JSONObject b = (JSONObject) _b;
				JSONArray beacon = new JSONArray();
				beacon.put(b.get("timestamp")).put("Beacon").put(info.get("absx")).put(info.get("absy"))
						.put(info.has("z") ? info.get("z") : 0).put(info.get("floor_num"));
				int count = 0;
				String uuid = b.getString("uuid");
				for (Object _d : b.getJSONArray("data")) {
					JSONObject d = (JSONObject) _d;
					Number rssi = (Number) d.get("rssi");
					if (rssi.intValue() < 0) {
						if (withUUID) {
							beacon.put(uuid + "-" + d.get("major") + "-" + d.get("minor"));
						} else {
							beacon.put(d.get("major"));
							beacon.put(d.get("minor"));
						}
						beacon.put(rssi);
						count++;
					}
				}
				beacon.put(6, count);
				array.add(beacon.join(","));
			}
		}
		return array;
	}

	private static JSONObject samplingToFeature(DBObject sp) throws JSONException {
		JSONObject feature = new JSONObject().put("type", "feature");
		DBObject info = (DBObject) sp.get("information");
		JSONArray coordinates = new JSONArray().put(info.get("lng")).put(info.get("lat"));
		JSONObject geometry = new JSONObject().put("type", "Point").put("coordinates", coordinates);
		feature.put("geometry", geometry);
		feature.put("properties", JSON.parse(info.toString()));
		if (sp.containsField("beacons")) {
			feature.put("beacons", JSON.parse((sp.get("beacons")).toString()));
		}
		return feature;
	}

	private static final long MB = 1024 * 1024;

	private static void memSize(String prefix) {
		long total = Runtime.getRuntime().totalMemory();
		long free = Runtime.getRuntime().freeMemory();
		long max = Runtime.getRuntime().maxMemory();
		System.out.println(String.format("%stotal %dMB, free %dMB, used %dMB, max %dMB", prefix, total / MB, free / MB,
				(total - free) / MB, max / MB));
	}

	private static class ArrayNWriter {
		private OutputStream os = null;
		private Writer writer = null;
		private GZIPOutputStream gzos = null;
		private String terminator, separator;
		private int count = 0;

		public ArrayNWriter(String starter, String terminator, String separator, HttpServletRequest request,
				HttpServletResponse response) {
			this.separator = separator;
			this.terminator = terminator;
			String acceptedEncodings = request.getHeader("accept-encoding");
			boolean gzip = acceptedEncodings != null && acceptedEncodings.indexOf("gzip") != -1;
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json");
			try {
				OutputStream _os = os = response.getOutputStream();
				if (gzip) {
					response.setHeader("Content-Encoding", "gzip");
					_os = gzos = new GZIPOutputStream(os);
				}
				writer = new BufferedWriter(new OutputStreamWriter(_os, "UTF-8"));
				writer.write(starter);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void write(Object data) throws Exception {
			if (separator != null && count++ > 0) {
				writer.write(separator);
			}
			if (data instanceof JSONArtifact) {
				((JSONArtifact) data).write(writer);
			} else {
				writer.write(data.toString());
			}
		}

		public void close() {
			try {
				writer.write(terminator);
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (gzos != null) {
				try {
					gzos.finish();
					gzos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}