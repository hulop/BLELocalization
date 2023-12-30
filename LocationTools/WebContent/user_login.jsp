<%@page import="org.apache.wink.json4j.JSONObject"%>
<jsp:useBean id="userBean" scope="request"
	class="hulo.commons.users.UserBean" />
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
	Object profile = userBean.login(request, response);
	if (profile == null) {
		return;
	}
	if (profile instanceof JSONObject) {
		profile = "OK";
	}
	String redirect_url = request.getParameter("redirect_url");
	if (redirect_url == null) {
		redirect_url = "";
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
<title>Login</title>
</head>
<body>
	<form action="user_login.jsp" method="post" autocomplete="off">
		<input type="hidden" name="redirect_url" value="<%=redirect_url%>">
		<div>
			<p>
				<label for="user"> User ID: </label> <br /> <input type="text"
					id="user" name="user" size="30" value="" />
			</p>
			<p>
				<label for="password"> Password: </label> <br /> <input
					type="password" id="password" name="password" size="30" value="" />
			</p>
			<p>
				<input value="Log in" type="submit" />
			</p>
			<p>
				<%=profile%>
			</p>
		</div>
	</form>
</body>
</html>