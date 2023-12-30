/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation
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

package hulo.commons.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class CORSServlet
 */
@WebServlet("/cors")
public class CORSServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CORSServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpURLConnection conn = null;
		InputStream is = null;
		OutputStream os = null;
		int responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		try {
			response.addHeader("Access-Control-Allow-Origin", "*");
			String url = request.getParameter("url");
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setRequestMethod("GET");
			conn.setUseCaches(false);
			responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				response.setContentType(conn.getContentType());
				int l = conn.getContentLength();
				if (l > 0) {
					response.setContentLength(l);
				}
				is = conn.getInputStream();
				os = response.getOutputStream();
				byte data[] = new byte[4096];
				int len = 0;
				while ((len = is.read(data, 0, data.length)) > 0) {
					os.write(data, 0, len);
				}
				os.flush();
				String urlText = url;
				String connUrl = conn.getURL().toString();
				if (!connUrl.equals(url)) {
					urlText += " => " + connUrl;
				}
				System.out.println(urlText + " \"" + conn.getContentType() + "\" " + conn.getContentLength());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			response.setStatus(responseCode);
			if (conn != null) {
				conn.disconnect();
			}
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
