/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation
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
package hulo.localization.servlet;

import hulo.localization.impl.BeaconLocalizer;

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

/**
 * Servlet implementation class BeaconLocationServlet
 */
@WebServlet(urlPatterns = { "/location/beacons" }, loadOnStartup = 1)
public class BeaconLocationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static HashSet<String> userList = new HashSet<String>();

	public static String[] getUsers() {
		return userList.toArray(new String[0]);
	}
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");

		final String action = request.getParameter("action");
		String user_id = request.getParameter("user_id");
		HttpSession session = request.getSession(true);

		if (user_id == null || user_id.isEmpty() || user_id.equals("null")) { // use just session
			user_id = session.getId();
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			//return;
		} else {
			String id = (String) session.getAttribute("id");
			if (id != null && 	!user_id.equals(id)) { // renew session if id is not matched
				session.invalidate();
				session = request.getSession(true);
			}
			session.setAttribute("id", user_id);
		}
		userList.add(user_id);

		final long start = System.currentTimeMillis();
		final String id = user_id;


		final BeaconLocalizer localizer = getLocalizer(session, user_id);

		new JSONBodyHandler() {
			@Override
			public Object getResult(Object source) throws JSONException {
				if ("reset".equals(action)) {
					if (source instanceof JSONObject) {
						System.out.println("reset:"+((JSONObject)source).toString());
						final JSONObject result = localizer.reset((JSONObject) source);
						return result;
					}
				} else if("correct".equals(action)){
					if (source instanceof JSONObject) {
						System.out.println("correct:"+((JSONObject)source).toString());
						final JSONObject result = localizer.correct((JSONObject) source);
						return result;
					}
				}else {
					if (source instanceof JSONArray) {
						final JSONObject result = localizer.update((JSONArray) source);
						long diff = System.currentTimeMillis() - start;
						System.out.println(diff+"ms");
						if (result != null && !result.containsKey("error")) {

							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										JSONObject dialogEvent = new JSONObject();
										dialogEvent.put("event", "location_sensor");
										dialogEvent.put("device_user", id);
										dialogEvent.put("indoor_location", result);
									} catch (JSONException e) {
										e.printStackTrace();
									}
								}
							}).start();
						}
						return result;
					}
				}
				return null;
			}
		}.exec(request, response);
	}


	private static BeaconLocalizer getLocalizer(HttpSession session, String user_id){
		BeaconLocalizer localizer = (BeaconLocalizer) session.getAttribute("localizer");
		if(localizer==null){
			System.out.println("Localizer for user "+user_id + " was created.");
			session.setAttribute("localizer", localizer = new BeaconLocalizer());
		}
		return localizer;
	}


	@Override
	public void init() throws ServletException {
		super.init();
		BeaconLocalizer.init();
	}

	@Override
	public void destroy() {
		super.destroy();
		BeaconLocalizer.destroy();
	}

}
