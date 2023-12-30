<%@page import="org.apache.wink.json4j.JSONObject"%>
<jsp:useBean id="userBean" scope="request" class="hulo.commons.users.UserBean" />
<jsp:useBean id="databaseBean" scope="request" class="hulo.commons.bean.DatabaseBean" />
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
String db = request.getParameter("db");
if (!databaseBean.existsDB(db)) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
	return;
}
String dbname = (db == null) ? "(default)" : "("+db+")";

JSONObject profile = (JSONObject)userBean.getProfile(request);
if (profile == null || !userBean.isDbAllowed(request, db)) {
	response.sendRedirect("user_login.jsp?logout=true&redirect_url=" + db + "/data_composer.jsp");
	return;
}
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
<title>Sampling data manager <%= dbname %></title>
<script type="text/javascript"
	src="js/lib/jquery/jquery-1.11.3.min.js"></script>
<script type="text/javascript" src="js/datautils.js"></script>
<script type="text/javascript"
	src="js/lib/OpenLayers-4.6.4/ol.js"></script>
<script type="text/javascript" src="js/util.js"></script>
<style>
table {
border:1px solid gray;
border-collapse: collapse;
}
table td {
border: 1px solid gray;
padding: 2px;
}
div#left {
width:60%;
float:left;
}
div#right {
width:40%;
float:right;
}
strong {
margin: 5px 0px;
display: block;
}
</style>
<script type="text/javascript" src="js/data_composer.js"></script>
<script type="text/javascript" src="js/hokoukukan.js"></script>
</head>
<body>
	<span style="font-weight: bold"><%=profile.getString("_id")%></span>
	<a href="data_composer.jsp?logout=true">log out</a> |
	<a href="user_password.jsp?redirect_url=data_composer.jsp">change password</a> |
	<a href="../">db list</a> | 
	<a href="floorplans.jsp">Manage Floor Plan</a> |
	<a href="sampling.jsp">Manage Samplings</a> |
	<span style="font-weight: bold">Data Compose Tool</span>
	<br>
<div><%= dbname %></div>
<div id="left"></div>
<div id="right"></div>
</body>
</html>
