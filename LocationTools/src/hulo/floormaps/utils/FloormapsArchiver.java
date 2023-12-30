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

package hulo.floormaps.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class FloormapsArchiver {

	private static final Pattern PAT_DATAURI = Pattern.compile("^data:(.*?);base64,(.*)", Pattern.CASE_INSENSITIVE);

	public static void export(String filename, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
		JSONArray floormaps = (JSONArray) JSON.parse(request.getParameter("floormaps"));
		OutputStream os = response.getOutputStream();
		write(floormaps, os);
	}

	public static void write(JSONArray floormaps, OutputStream os) throws JSONException, IOException {
		assert PAT_DATAURI != null;
		try (ZipOutputStream zos = new ZipOutputStream(os)) {
			for (Object obj : floormaps) {
				JSONObject fp = (JSONObject) obj;
				if (fp.has("image")) {
					Matcher m = PAT_DATAURI.matcher(fp.getString("image"));
					if (m.matches()) {
						byte[] bytes = Base64.getDecoder().decode(m.group(2));
						String filename = String.format("map/%s.png", fp.getString("id"));
						zos.putNextEntry(new ZipEntry(filename));
						zos.write(bytes);
						zos.closeEntry();
						fp.put("image", filename);
					}
				}
			}
			zos.putNextEntry(new ZipEntry("map/floormaps.json"));
			zos.write(floormaps.toString(4).getBytes("UTF-8"));
			zos.closeEntry();
		}
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			String inFile = args[0];
			String outFile = inFile.replaceAll("\\..*$", ".zip");
			System.out.format("converting %s to %s...\n", inFile, outFile);
			try (InputStream is = new FileInputStream(args[0])) {
				JSONArray floormaps = (JSONArray) JSON.parse(is);
				try (OutputStream os = new FileOutputStream(outFile)) {
					write(floormaps, os);
					System.out.println("done");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		System.err.println("error: please specify floormaps json file");
	}

}
