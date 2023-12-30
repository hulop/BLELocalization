<%@page import="hulo.localization.servlet.MongoService"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.wink.json4j.JSONArray"%>
<%@page import="org.apache.wink.json4j.JSONObject"%>
<jsp:useBean id="databaseBean" scope="request"
	class="hulo.commons.bean.DatabaseBean" />
<jsp:useBean id="userBean" scope="request"
	class="hulo.commons.users.UserBean" />
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
	String user = request.getParameter("edit_user");
	String redirect_url = request.getParameter("redirect_url");
	if (user == null || redirect_url == null || !userBean.isAdmin(request)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
		return;
	}
	JSONObject obj = userBean.findUser(user);
	if (obj == null) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
		return;
	}
	JSONArray objDblist = obj.getJSONArray("dblist");
	if (objDblist == null) {
		objDblist = new JSONArray();
	}
	String message = "";
	String password = request.getParameter("password");
	String password2 = request.getParameter("password2");
	String admin = request.getParameter("admin");

	String savePassword = null;
	String[] saveDblist = request.getParameterValues("dblist");
	if (password != null && password.length() > 0) {
		if (password.equals(password2)) {
			savePassword = password;
		} else {
			message = "Please enter same password";
		}
	}
	if (request.getParameter("save-user") != null) {
		userBean.updateUser(user, savePassword, admin != null, saveDblist == null ? new String[0] : saveDblist);
		response.sendRedirect(redirect_url);
		return;
	}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!----------------------------------------------------------------------------
Copyright (c) 2014, 2023 IBM Corporation
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
------------------------------------------------------------------------------>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Change user profile</title>
</head>
<body>
	<div>
		<form method="post" action="user_profile.jsp">
			<input type="hidden" name="edit_user" value="<%=user%>"> <input
				type="hidden" name="redirect_url" value="<%=redirect_url%>">
			<table>
				<tbody>
					<tr>
						<td>password:</td>
						<td><input type="password" name="password" /></td>
					</tr>
					<tr>
						<td>confirm password:</td>
						<td><input type="password" name="password2" /></td>
					</tr>
					<tr>
						<td>role:</td>
						<td><label><input type="checkbox" name="admin"
								<%=obj.getBoolean("admin") ? "checked" : ""%> />administrator</label></td>
					</tr>
					<tr>
						<td>db:</td>
						<td>
							<%
								for (String name : databaseBean.getDBNames()) {
							%> <input type="checkbox" name="dblist" value="<%=name%>"
							<%=objDblist.indexOf(name) != -1 ? "checked" : ""%> /><%=name.replace(MongoService.DB_NAME_PREFIX, "")%><br>
							<%
								}
							%>
						</td>
					</tr>
					<tr>
						<td colspan="2" align="right">
							<button onclick="location.href='<%=redirect_url%>';return false;">Cancel</button>
							<input type="submit" name="save-user" value="Save">
						</td>
					</tr>
				</tbody>
			</table>
		</form>
	</div>
</body>
</html>