package hulo.localization.servlet;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.math3.stat.descriptive.moment.GeometricMean;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

/**
 * Servlet implementation class DataServlet
 * 
 * 
 * <pre>
 *  GET - Get resource with or without id
 *  
 *  URI:
 *      GET /data/file - Get list of files
 *      GET /data/file/filePath - Get a file
 *      GET /data/collectionName - Get all documents
 *      GET /data/collectionName/filePath - Get a document with filePath
 *  
 *  Optional parameters for DB:
 *      keys: Fields to return
 *      
 *  (when id is not specified)
 *      distinct: Field for which to return distinct value
 *      
 *      pipeline: Pipeline object
 *      
 *      query: Object for which to search
 *      sort: Object for which to sort
 *      skip: Number of elements to skip
 *      limit: Number of elements to return
 *      count: Returns number of elements instead of DB Object
 *  
 * -------- 
 * 
 *  POST - Insert a new resource or perform special action
 *  
 *  URI:
 *      POST /data/file/filePath - Create a file (file in POST body)
 *      POST /data/collectionName - Insert a document (document in data parameter)
 *  (when action parameter is "update")
 *      POST /data/collectionName - Update a document with query and update parameter
 *      POST /data/collectionName/id - Update a document with id and update parameter
 *  
 *  Optional parameters:
 *      action: Document update action
 *      
 *  (when action parameter is "update")
 *      query: Object for which to search (not used when id is specified)
 *      update: Update action object
 *  
 * --------
 *  
 *  PUT - Update a resource with id
 *  
 *  URI:
 *      PUT /data/file/filePath - Update a file (file in POST body)
 *      PUT /data/collectionName - Update a document (document with id in data parameter)
 *      PUT /data/collectionName/id - Update a document with id (document in data parameter)
 *  
 * -------- 
 * 
 *  DELETE - Delete a resource with id
 *  
 *  URI:
 *      DELETE /data/file/filePath - Delete a file
 *      DELETE /data/collectionName/id - Delete a document
 * </pre>
 */

@WebServlet("/data")
@MultipartConfig()
public class DataServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private MongoService mDS = MongoService.getInstance();
	private DBCollection mCollRef = mDS.getCollection("refpoints"), 
			mCollSamp = mDS.getCollection("samplings"),
			mCollMap = mDS.getCollection("maps");

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DataServlet() {
		super();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		onRefpointChange(null);
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		super.service(request, response);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 * 
	 *      Get resource
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		if (mDS != MongoService.getInstance()) {
			System.out.println("MongoService db name is changed");
			mDS = MongoService.getInstance();
			mCollRef = mDS.getCollection("refpoints");
			mCollSamp = mDS.getCollection("samplings");
			mCollMap = mDS.getCollection("maps");
		}
		
		String type = request.getParameter("type");
		String id = request.getParameter("id");
		System.out.println("doGet: type=" + type + " id=" + id);

		if ("file".equals(type)) {
			if (id == null) {
				// Get list of files
				mDS.sendJSON(mDS.getFileList(), request, response);
				return;
			}
			try {
				mDS.sendFile(id, response);
			} catch (Exception e) {
				System.err.println("Send error: " + id);
			}
			return;
		}

		// Get document(s)
		String distinct = request.getParameter("distinct");
		String pipeline = request.getParameter("pipeline");
		String query = request.getParameter("query");
		String keys = request.getParameter("keys");
		String format = request.getParameter("format");
		DBObject queryObj = query != null ? (DBObject) JSON.parse(query) : null;
		DBObject keysObj = keys != null ? (DBObject) JSON.parse(keys) : null;

		Object result = null;
		DBCollection collection = mDS.getCollection(type);
		if (id != null) {
			result = collection.findOne(new ObjectId(id), keysObj);
		} else if (distinct != null) {
			result = collection.distinct(distinct, queryObj);
		} else if (pipeline != null) {
			DBObject pipelineObj = (DBObject) JSON.parse(pipeline);
			if (pipelineObj instanceof List<?>) {
				result = collection.aggregate((List<DBObject>) pipelineObj).results();
			} else {
				result = collection.aggregate(pipelineObj).results();
			}
		} else {
			DBCursor cursor = collection.find(queryObj, keysObj);
			String sort = request.getParameter("sort");
			String skip = request.getParameter("skip");
			String limit = request.getParameter("limit");
			String count = request.getParameter("count");
			if (sort != null) {
				cursor = cursor.sort((DBObject) JSON.parse(sort));
			}
			if (skip != null) {
				cursor = cursor.skip(Integer.parseInt(skip));
			}
			if (limit != null) {
				cursor = cursor.limit(Integer.parseInt(limit));
			}
			result = "true".equals(count) ? cursor.count() : cursor;
		}
		if (id != null && result == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("Document %s does not exist", id));
			return;
		}
		if ("csv".equals(format)) {
			mDS.sendCSV(result, request, response);
			return;
		}
		mDS.sendJSON(result, request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 * 
	 *      Insert a new resource
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String type = request.getParameter("type");
		String id = request.getParameter("id");
		System.out.println("doPost: type=" + type + " id=" + id);

		if ("file".equals(type)) {
			// Save a file with id
			doPut(request, response);
			return;
		}

		DBCollection collection = mDS.getCollection(type);
		String action = request.getParameter("action");
		if (action != null) {
			// Manipulate database
			String query = request.getParameter("query");
			DBObject queryObj = null;
			if (id != null) {
				queryObj = new BasicDBObject("_id", new ObjectId(id));
			} else if (query != null) {
				queryObj = (DBObject) JSON.parse(query);
			}
			if ("update".equals(action)) {
				String update = request.getParameter("update");
				DBObject updateObj = update != null ? (DBObject) JSON.parse(update) : null;
				if (queryObj == null || updateObj == null) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No query or update parameters");
					return;
				}
				String upsert = request.getParameter("upsert");
				String multi = request.getParameter("multi");
				mDS.sendJSON(collection.update(queryObj, updateObj, "true".equals(upsert), "true".equals(multi)), request, response);
				if ("samplings".equals(type)) {
					onSamplingChange(queryObj);
				}
			} else if ("remove".equals(action)) {
				if (queryObj == null) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No query parameter");
					return;
				}
				mDS.sendJSON(collection.remove(queryObj), request, response);
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("Unknown action %s", action));
			}
			return;
		}

		// Insert a document
		DBObject data = getDataObject(request);
		if (data == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No data specified");
			return;
		}
		Object dataID = data.get("_id");
		if (dataID != null && collection.findOne(dataID) != null) {
			// response.sendError(HttpServletResponse.SC_BAD_REQUEST,
			// "Duplicated id");
			// return;
		}
		// collection.insert(data);
		if ("samplings".equals(type)) {
			updateSamplingData(data);
		}
		collection.save(data);
		mDS.sendJSON(data, request, response);
		if ("refpoints".equals(type) && dataID != null) {
			onRefpointChange(new BasicDBObject("_id", dataID));
		}
		response.setStatus(HttpServletResponse.SC_OK);
		response.getOutputStream().close();
	}

	/**
	 * @see HttpServlet#doPut(HttpServletRequest, HttpServletResponse)
	 * 
	 *      Update a resource with id
	 */
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String type = request.getParameter("type");
		String id = request.getParameter("id");
		System.out.println("doPut: type=" + type + " id=" + id);

		if ("file".equals(type)) {
			// Save a file
//			if (id == null) {
//				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file id specified");
//				return;
//			}
			InputStream is = null;
			try {
				Part part = getPart(request, "file");
				String contentType = null;
				if (part == null) {
					is = request.getInputStream();
					contentType = request.getContentType();
				} else {
					is = part.getInputStream();
					contentType = part.getContentType(); // TODO
				}
				if (id == null) {
					if ((id = mDS.saveFile(is, contentType)) != null) {											
						mDS.sendJSON(mDS.getFile(id), request, response);
					}
				}
				else {
					if (mDS.saveFile(id, is, contentType)) {
						mDS.sendJSON("OK", request, response);
					}
				}
			} finally {
				if (is != null) {
					is.close();
				}
			}
			return;
		}

		// Update a document
		DBObject data = getDataObject(request);
		if (data == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No data specified");
			return;
		}
		Object idObj = data.get("_id"); // = id in source
		if (idObj == null && id != null) {
			idObj = new ObjectId(id); // = id in URI
		}
		if (idObj == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No document id specified");
			return;
		}
		DBCollection collection = mDS.getCollection(type);
		collection.update(new BasicDBObject("_id", idObj), data, true, false);
		mDS.sendJSON(data, request, response);
	}

	/**
	 * @see HttpServlet#doDelete(HttpServletRequest, HttpServletResponse)
	 * 
	 *      Delete a resource
	 */
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String type = request.getParameter("type");
		String id = request.getParameter("id");
		String query = request.getParameter("query");
		
		System.out.println("doDelete: type=" + type + " id=" + id + " query="+query);

		
		if (query != null) {
			DBCollection collection = mDS.getCollection(type);
			DBObject queryObj = (DBObject) JSON.parse(query);
			DBCursor cursor = collection.find(queryObj);
			System.out.println("found "+cursor.count());

			ArrayList<Object> list = new ArrayList<Object>();
			while(cursor.hasNext()) {
				DBObject obj = cursor.next();
				list.add(obj.get("_id"));
			}
			DBObject ids = new BasicDBObject();
			DBObject in = new BasicDBObject();
			in.put("$in", list);
			ids.put("_id", in);
			
			WriteResult result = collection.remove(ids);			
			mDS.sendJSON(result, request, response);
			return;
		}
		
		if (id == null ) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Delete all is not supported");
			return;
		}

		if ("file".equals(type)) {
			// Delete a file
			if (mDS.isFileExists(id)) {
				if (mDS.deleteFile(id)) {
					mDS.sendJSON("OK", request, response);
				}
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("File %s does not exist", id));
			}
			return;
		}

		// Delete a document
		DBCollection collection = mDS.getCollection(type);
		DBObject obj = collection.findOne(new ObjectId(id));
		if (obj == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("Document %s does not exist", id));
			return;
		}
		mDS.sendJSON(collection.remove(obj), request, response);
	}

	private static DBObject getDataObject(HttpServletRequest request) throws IOException, ServletException {
		for (Enumeration<String> en = request.getParameterNames(); en.hasMoreElements();) {
			String name = en.nextElement();
			System.out.println(name + ": " + request.getParameter(name));
		}
		DBObject data = getDBObject(request, "data");
		if (!(data instanceof BasicDBObject)) {
			return null;
		}
		if (data != null) {
			DBObject md = getDBObject(request, "_metadata");
			if (md == null) {
				md = new BasicDBObject();
			}
			md.put("created_at", new Date());
			data.put("_metadata", md);
		}
		return data;
	}

	private static DBObject getDBObject(HttpServletRequest request, String name) throws IOException, ServletException {
		String data = request.getParameter(name);
		if (data == null) {
			// for multipart data
			Part part = getPart(request, name);
			if (part != null) {
				InputStream is = null;
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(new InputStreamReader(is = part.getInputStream()));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						sb.append(line);
						sb.append("\n");
					}
					data = sb.toString();
				} finally {
					if (is != null) {
						is.close();
					}
					if (reader != null) {
						reader.close();
					}
				}
			}
		}
		try {
			return data != null ? (DBObject) JSON.parse(data) : null;
		} catch (Exception e) {
			System.err.println(e.getClass().getName());
			return null;
		}
	}

	private static Part getPart(HttpServletRequest request, String name) {
		try {
			return request.getPart(name);
		} catch (Exception e) {
			return null;
		}
	}

	private void onRefpointChange(DBObject queryRef) {
		for (DBCursor refpoints = mCollRef.find(queryRef); refpoints.hasNext();) {
			DBObject refpoint = refpoints.next();
			if (refpoint.containsField("_id") && refpoint.containsField("x") && refpoint.containsField("y") && refpoint.containsField("floor")
					&& refpoint.containsField("floor_num")) {
				for (DBCursor samplings = mCollSamp.find(new BasicDBObject("information.refid", refpoint.get("_id")), new BasicDBObject("information", 1)); samplings
						.hasNext();) {
					updateSampleInfo(samplings.next(), refpoint);
				}
			}
		}
	}

	private void onSamplingChange(DBObject querySample) {
		DBObject keys = new BasicDBObject("information", 1);
		for (DBCursor samplings = mCollSamp.find(querySample, keys); samplings.hasNext();) {
			updateSampleInfo(samplings.next(), null);
		}
	}

	private void updateSampleInfo(DBObject sampling, DBObject refpoint) {
		DBObject info = (DBObject) sampling.get("information");
		if (info.containsField("refid") && info.containsField("x") && info.containsField("y")) {
			if (refpoint == null) {
				refpoint = mCollRef.findOne(new BasicDBObject("_id", info.get("refid")));
				if (refpoint == null) {
					System.err.println("No refpoint: " + JSON.serialize(info));
					return;
				}
			}
			DBObject set = new BasicDBObject();
			AffineTransform at = new AffineTransform();
			at.translate(((Number) refpoint.get("x")).doubleValue(), ((Number) refpoint.get("y")).doubleValue());
			at.rotate(Math.toRadians(((Number) refpoint.get("rotate")).doubleValue()));
			Point2D.Double src = new Point2D.Double(((Number) info.get("x")).doubleValue(), ((Number) info.get("y")).doubleValue());
			Point2D.Double dst = new Point2D.Double();
			at.transform(src, dst);
			set.put("information.absx", dst.getX());
			set.put("information.absy", dst.getY());
			set.put("information.floor", refpoint.get("floor"));
			set.put("information.floor_num", refpoint.get("floor_num"));
			mCollSamp.update(new BasicDBObject("_id", sampling.get("_id")), new BasicDBObject("$set", set));
			System.out.println(JSON.serialize(set));
		}

	}

	private void updateSamplingData(DBObject data) {
		if (data.containsField("information")) {
			DBObject info = (DBObject) data.get("information");
		
			if (info.containsField("refid") && info.containsField("x") && info.containsField("y")) {
				DBObject refpoint = mCollRef.findOne(info.get("refid"));
				if (refpoint != null && refpoint.containsField("x") && refpoint.containsField("y") && refpoint.containsField("floor")
						&& refpoint.containsField("floor_num")) {
					info.put("absx", ((Number) info.get("x")).doubleValue() + ((Number) refpoint.get("x")).doubleValue());
					info.put("absy", ((Number) info.get("y")).doubleValue() + ((Number) refpoint.get("y")).doubleValue());
					info.put("floor", refpoint.get("floor"));
					info.put("floor_num", refpoint.get("floor_num"));
					System.out.println(JSON.serialize(info));
				} else {
					info.put("absx", ((Number) info.get("x")).doubleValue());
					info.put("absy", ((Number) info.get("y")).doubleValue());
				}
			}
		}
	}
}
