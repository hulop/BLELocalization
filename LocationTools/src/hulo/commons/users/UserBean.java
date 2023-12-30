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

package hulo.commons.users;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import hulo.commons.bean.DatabaseBean;
import hulo.localization.servlet.MongoService;

public class UserBean {

	private static final UserDB adapter = new UserDB();

	public UserBean() {
		JSONArray users = listUsers();
		if (users.length() == 0) {
			addUser("hulopadmin", "please change password", true, new String[0]);
		}
	}

	public Object login(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String user = request.getParameter("user");
		String password = request.getParameter("password");
		Object user_profile = user != null && password != null ? createProfile(user, password) : getProfile(request);

		if (user_profile != null) {
			HttpSession session = request.getSession(true);
			session.setAttribute("user_profile", user_profile);
			String redirect_url = request.getParameter("redirect_url");
			if (redirect_url != null && redirect_url.length() > 0) {
				response.sendRedirect(redirect_url);
				return null;
			}
			return user_profile;
		} else {
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
			return user != null ? "INVALID_CREDENTIAL" : "";
		}
	}

	public Object getProfile(HttpServletRequest request) throws Exception {
		HttpSession session = request.getSession(false);
		if (session != null) {
			if (request.getParameter("logout") == null) {
				return session.getAttribute("user_profile");
			}
			session.invalidate();
		}
		return null;
	}

	public Object createProfile(String user, String password) {
		try {
			JSONObject obj = adapter.findUser(user);
			if (obj != null && getHash(user, password).equals(obj.get("password"))) {
				return obj;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public JSONArray listUsers() {
		try {
			return adapter.listUsers();
		} catch (JSONException e) {
			e.printStackTrace();
			return new JSONArray();
		}
	}

	public JSONObject findUser(String user) {
		try {
			return adapter.findUser(user);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void addUser(String user, String password, Boolean admin, String[] dblist) {
		JSONObject obj = new JSONObject();
		try {
			obj.put("_id", user);
			obj.put("password", getHash(user, password));
			obj.put("admin", admin);
			obj.put("dblist", dblist);
			adapter.updateUser(obj.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void updateUser(String user, String password, Boolean admin, String[] dblist) {
		try {
			JSONObject obj = adapter.findUser(user);
			if (obj != null) {
				if (password != null) {
					obj.put("password", getHash(user, password));
				}
				if (admin != null) {
					obj.put("admin", admin);
				}
				if (dblist != null) {
					obj.put("dblist", dblist);
				}
				adapter.updateUser(obj.toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void removeUser(String user) {
		adapter.removeUser(user);
	}

	public String getHash(String user, String password) {
		return DigestUtils.sha256Hex(DigestUtils.sha256Hex(user) + password);
	}

	public boolean isAdmin(HttpServletRequest request) {
		Object admin = getSessionValue(request, "admin");
		return (admin instanceof Boolean) && (Boolean) admin;
	}

	public boolean isDbAllowed(HttpServletRequest request, String db_name) {
		if (isAdmin(request)) {
			return true;
		}
		Object dblist = getSessionValue(request, "dblist");
		return (dblist instanceof JSONArray) && ((JSONArray) dblist).contains(MongoService.DB_NAME_PREFIX + db_name);
	}

	public List<String> getDBNames(HttpServletRequest request) {
		List<String> list = new ArrayList<>();
		JSONArray dblist = (JSONArray) getSessionValue(request, "dblist");
		if (isAdmin(request)) {
			return DatabaseBean.getDBNames();
		}
		for (String name : DatabaseBean.getDBNames()) {
			if (dblist.contains(name)) {
				list.add(name);
			}
		}
		return list;
	}

	public JSONArray getDBList(HttpServletRequest request) {
		List<String> list = new ArrayList<>();
		JSONArray dblist = (JSONArray) getSessionValue(request, "dblist");
		if (isAdmin(request)) {
			list = DatabaseBean.getDBNames();
		} else {
			for (String name : DatabaseBean.getDBNames()) {
				if (dblist.contains(name)) {
					list.add(name);
				}
			}
		}
		list.removeIf(new Predicate<String>() {
			public boolean test(String t) {
				return t.equals(MongoService.DB_NAME_PREFIX);
			}
		});
		JSONArray info_list = new JSONArray();
		for (String db_name : list) {
			db_name = db_name.replace(MongoService.DB_NAME_PREFIX, "");
			try {
				JSONObject obj = adapter.findInfo(db_name);
				if (obj == null) {
					obj = new JSONObject().put("_id", db_name);
				}
				info_list.add(obj);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return info_list;
	}
	
	public void updateInfo(Object info) {
		adapter.updateInfo(info.toString());
	}

	private Object getSessionValue(HttpServletRequest request, String name) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			try {
				JSONObject user_profile = (JSONObject) session.getAttribute("user_profile");
				return user_profile != null ? user_profile.get(name) : null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
