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
	response.sendRedirect("user_login.jsp?logout=true&redirect_url=" + db + "/");
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
<meta name="copyright" content="Copyright (c) IBM Corporation 2014, 2015. This page is made available under MIT license.">
<title>HULOP Tools <%= dbname %></title>
<style type="text/css">
dt {
	font-size: 16px;
	font-weight: bold;
}

dd {
	margin-left: 0px;
	margin-bottom: 2em;
}

li {
	margin-bottom: 10px;
}
</style>
</head>
<body>
	<span style="font-weight: bold"><%=profile.getString("_id")%></span>
	<a href="./?logout=true">log out</a> |
	<a href="user_password.jsp?redirect_url=./">change password</a> | 
	<a href="../">db list</a>
	<br>

	<div>
		<dl>			
			<dt>HULOP Tools <%= dbname %></dt>
			<dd>
			  <ul>
			    <li><a href="floorplans.jsp">Manage Floor Plan</a></li>
			    <li><a href="sampling.jsp">Manage Samplings</a></li>
			    <li><a href="data_composer.jsp">Data Compose Tool</a></li>
			    <!--  <li><a href="dbs.html">DB switch</a></li>-->
			  </ul>
			</dd>
			
		</dl>
	</div>

</body>
</html>
