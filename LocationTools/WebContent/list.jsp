<jsp:useBean id="userBean" scope="request"
	class="hulo.commons.users.UserBean" />
<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@page import="org.apache.wink.json4j.JSON"%>
<%@page import="org.apache.wink.json4j.JSONObject"%>
<%@page import="org.apache.wink.json4j.JSONArray"%>
<%
	if ("POST".equals(request.getMethod())) {
		try {
			Object data = JSON.parse(request.getInputStream());
			if (data instanceof JSONArray) {
				for (Object obj : (JSONArray) data) {
					userBean.updateInfo(obj);
				}
				response.getWriter().print("OK");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}
	JSONObject profile = (JSONObject) userBean.getProfile(request);
	if (profile == null) {
		response.sendRedirect("user_login.jsp?logout=true&redirect_url=.");
		return;
	}
	String loginUser = profile.getString("_id");
	boolean isAdmin = userBean.isAdmin(request);
	JSONArray list = userBean.getDBList(request);
%>
<!DOCTYPE html>
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
<meta charset="UTF-8">
<meta name="copyright"
	content="Copyright (c) IBM Corporation 2014, 2015. This page is made available under MIT license.">
<title>DBs</title>
<script type="text/javascript" src="js/lib/jquery/jquery-1.11.3.min.js"></script>
<link rel="stylesheet" type="text/css"
	href="js/lib/jquery-ui-1.11.4/jquery-ui.min.css">
<script type="text/javascript"
	src="js/lib/jquery-ui-1.11.4/jquery-ui.min.js"></script>
<link rel="stylesheet" type="text/css"
	href="js/lib/DataTables-1.10.10/media/css/jquery.dataTables.css">
<script type="text/javascript"
	src="js/lib/DataTables-1.10.10/media/js/jquery.dataTables.min.js"></script>
<style type="text/css">
[contenteditable="true"] {
	border: solid 0.1px;
	background-color: white !important;
}
</style>
<script type="text/javascript" src="js/list.js"></script>
</head>
<body>
	<div>
		<span style="font-weight: bold"><%=profile.getString("_id")%></span> <a
			href=".?logout=true">log out</a> | <a
			href="user_password.jsp?redirect_url=.">change password</a> | <span
			style="font-weight: bold">db list</span>
		<%
			if (isAdmin) {
		%>
		| <a href="user_admin.jsp">administration</a> |
		<button id="edit_group">Edit</button>
		<button id="cancel_group">Cancel</button>
		<button id="save_group">Save</button>
		<%
			}
		%>
	</div>
	<h1 class="ui-widget-header">Databases</h1>
	<div class="ui-widget-content">
		<table id="table" class="display">
			<thead>
				<tr>
					<td>DB Name</td>
					<td>Group</td>
				</tr>
			</thead>
			<tbody>
				<%
					for (int i = 0; i < list.length(); i++) {
						JSONObject obj = list.getJSONObject(i);
						String db = obj.optString("_id", "");
						String group = obj.optString("group", "");
				%>
				<tr>
					<td class="db_name"><a href="<%=db%>/floorplans.jsp"><%=db%></a></td>
					<td class="group_name editable"><%=group%></td>
				</tr>
				<%
					}
				%>
			</tbody>
		</table>
	</div>
	<%
		if (isAdmin) {
	%>
	<br>
	<div>
		<fieldset>
			<legend>Create a new Database</legend>
			<label>DB Name: <input type="text" id="dbname"></label> <label>Group:
				<input type="text" id="group_name">
			</label>
			<button id="adddb">Add</button>
		</fieldset>
	</div>
	<%
		}
	%>
</body>
</html>
