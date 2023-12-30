<%@page import="hulo.localization.servlet.MongoService"%>
<%@page import="org.apache.wink.json4j.JSONArray"%>
<%@page import="org.apache.wink.json4j.JSONObject"%>
<jsp:useBean id="databaseBean" scope="request"
	class="hulo.commons.bean.DatabaseBean" />
<jsp:useBean id="userBean" scope="request"
	class="hulo.commons.users.UserBean" />
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
	JSONObject profile = (JSONObject) userBean.getProfile(request);
	if (profile == null || !userBean.isAdmin(request)) {
		response.sendRedirect("user_login.jsp?logout=true&redirect_url=user_admin.jsp");
		return;
	}

	String user = request.getParameter("user");
	if (user != null) {
		if (request.getParameter("add-user") != null) {
			String password = request.getParameter("password");
			String password2 = request.getParameter("password2");
			String admin = request.getParameter("admin");
			String[] dblist = request.getParameterValues("dblist");
			if (password != null && password.equals(password2)) {
				userBean.addUser(user, password, admin != null, dblist);
			}
		} else if (request.getParameter("remove-user") != null) {
			userBean.removeUser(user);
		} else if (request.getParameter("edit-user") != null) {
			response.sendRedirect("user_profile.jsp?redirect_url=user_admin.jsp&edit_user=" + user);
		}
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
<title>Administrate Users</title>
</head>
<body>
	<span style="font-weight: bold"><%=profile.getString("_id")%></span>
	<a href="user_admin.jsp?logout=true">log out</a> |
	<a href="user_password.jsp?redirect_url=user_admin.jsp">change password</a> | 
	<a href="./">db list</a> |
	<span style="font-weight: bold">administration</span>
	<br>
	<fieldset>
		<legend>Add a user</legend>
		<form method="post" id="add-user">
			<table>
				<tbody>
					<tr>
						<td>user:</td>
						<td><input type="text" name="user" /></td>
					</tr>
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
						<td><label><input type="checkbox" name="admin" />administrator</label></td>
					</tr>
					<tr>
						<td>db:</td>
						<td>
							<%
								for (String name : databaseBean.getDBNames()) {
							%> <label><input type="checkbox" name="dblist"
								value="<%=name%>"><%=name.replace(MongoService.DB_NAME_PREFIX, "")%></label><br>
							<%
								}
							%>
						</td>
					</tr>
					<tr>
						<td colspan="2" align="right"><input type="submit"
							name="add-user" value="Add a user"></td>
					</tr>
				</tbody>
			</table>
		</form>
	</fieldset>
	<fieldset>
		<legend>Users</legend>
		<div id="userList">
			<table border="1">
				<thead>
					<tr>
						<th>user</th>
						<th>role</th>
						<th>db</th>
						<th>action</th>
					</tr>
				</thead>
				<tbody>
					<%
						for (Object obj : userBean.listUsers()) {
							JSONObject o = (JSONObject) obj;
							StringBuilder sb = new StringBuilder();
							for (Object nam : o.getJSONArray("dblist")) {
								if (sb.length() > 0) {
									sb.append(",");
								}
								sb.append(nam.toString().replace(MongoService.DB_NAME_PREFIX, ""));
							}
					%>
					<tr>
						<td><%=o.getString("_id")%></td>
						<td><%=o.getBoolean("admin") ? "administrator" : ""%></td>
						<td><%=sb.toString()%></td>
						<td>
							<form method="post">
								<input type="hidden" name="user" value="<%=o.getString("_id")%>">
								<input type="submit" name="remove-user" value="remove"><input
									type="submit" name="edit-user" value="edit">
							</form>
						</td>
					</tr>
					<%
						}
					%>
				</tbody>
			</table>
		</div>
	</fieldset>
</body>
</html>