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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class ModelArchiver {

	private static final Pattern PAT_DATAURI = Pattern.compile("^data:(.*?);base64,(.*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern PAT_JSON = Pattern.compile("^[\\{\\[]");

	private static JSONObject attach(String data, String filename, JSONObject ref, ZipOutputStream zos)
			throws JSONException, IOException {
		assert PAT_DATAURI != null;
		assert PAT_JSON != null;
		final String type;
		final byte[] bytes;
		Matcher m = PAT_DATAURI.matcher(data);
		if (m.matches()) {
			bytes = Base64.getDecoder().decode(m.group(2));
			type = m.group(1);
		} else {
			bytes = data.getBytes();
			type = PAT_JSON.matcher(data).find() ? "application/json" : "text/csv";
		}
		if (filename.indexOf('.') == -1) {
			String[] part = type.split("\\/");
			filename += "." + part[part.length - 1];
		}
		zos.putNextEntry(new ZipEntry(filename));
		zos.write(bytes);
		zos.closeEntry();
		ref.put("src", filename);
		ref.put("type", type);
		return ref;
	}

	private static JSONObject attach(boolean json, List<byte[]> data, String filename, JSONObject ref,
			ZipOutputStream zos, boolean withUUID) throws JSONException, IOException {
		final String type = json ? "application/json" : "text/csv";
		if (filename.indexOf('.') == -1) {
			String[] part = type.split("\\/");
			filename += "." + part[part.length - 1];
		}
		zos.putNextEntry(new ZipEntry(filename));
		boolean separate = false;
		if (json) {
			byte[] EOL = ",\n".getBytes();
			zos.write("[".getBytes());
			for (byte[] bytes : data) {
				if (separate) {
					zos.write(EOL);
				}
				zos.write(bytes);
				separate = true;
			}
			zos.write("]".getBytes());
		} else {
			byte[] EOL = "\n".getBytes();
			for (byte[] bytes : data) {
				String sampling = new String(bytes, "UTF-8");
				for (Object obj : ExportUtils.samplingToCSV((JSONObject) JSON.parse(sampling), withUUID)) {
					if (separate) {
						zos.write(EOL);
					}
					zos.write(obj.toString().getBytes("UTF-8"));
					separate = true;
				}
			}
		}
		zos.closeEntry();
		ref.put("src", filename);
		ref.put("type", type);
		return ref;
	}

	public static void write(JSONObject model, List<byte[]> samples_data, List<byte[]> samples_raw_data,
			OutputStream os, boolean withUUID) throws JSONException, IOException {
		try (ZipOutputStream zos = new ZipOutputStream(os)) {
			for (Iterator<String> it = model.keys(); it.hasNext();) {
				String key = it.next();
				Object root = model.get(key);
				if (root instanceof JSONArray) {
					int index = 0;
					for (Object child : (JSONArray) root) {
						if (child instanceof JSONObject) {
							JSONObject file = (JSONObject) child;
							Object data = file.remove("data");
							if (data != null) {
								attach(data.toString(), key + "/" + ++index, file, zos);
							}
						}
					}
				}
			}
			if (samples_data != null) {
				JSONArray samples = new JSONArray();
				samples.put(attach(false, samples_data, "samples/1", new JSONObject(), zos, withUUID));
				samples.put(attach(true, samples_data, "samples/1", new JSONObject(), zos, false));
				model.put("samples", samples);
			}
			if (samples_raw_data != null) {
				JSONArray samples_raw = new JSONArray();
				samples_raw.put(attach(true, samples_raw_data, "samples_raw/1", new JSONObject(), zos, false));
				model.put("samples_raw", samples_raw);
			}
			zos.putNextEntry(new ZipEntry("manifest.json"));
			model.write(zos, 4);
			zos.closeEntry();
		}
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			String inFile = args[0];
			String outFile = inFile.replaceAll("\\..*$", ".zip");
			System.out.format("converting %s to %s...\n", inFile, outFile);
			try (InputStream is = new FileInputStream(args[0])) {
				JSONObject model = (JSONObject) JSON.parse(is);
				try (OutputStream os = new FileOutputStream(outFile)) {
					write(model, null, null, os, false);
					System.out.println("done");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		System.err.println("error: please specify model json file");
	}
}
