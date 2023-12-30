<%@page import="org.apache.wink.json4j.JSONObject"%>
<jsp:useBean id="userBean" scope="request"
	class="hulo.commons.users.UserBean" />
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
	JSONObject profile = (JSONObject)userBean.getProfile(request);
	String redirect_url = request.getParameter("redirect_url");
	if (profile == null || redirect_url == null) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
		return;
	}
	String message = "";
	String user = profile.getString("_id");
	String password = request.getParameter("password");
	String password2 = request.getParameter("password2");
	if (password != null && password.length() > 0) {
		if (password.equals(password2)) {
			userBean.updateUser(user, password, null, null);
			response.sendRedirect(redirect_url);
			return;
		} else {
			message = "Please enter same password";
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
<meta name="copyright"
	content="Copyright (c) IBM Corporation and others 2014, 2017. This page is made available under MIT license.">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Change password</title>
</head>
<body>
	<div>
		<form method="post" action="user_password.jsp">
			<input type="hidden" name="redirect_url" value="<%=redirect_url%>">
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
						<td colspan="2" align="right">
							<button onclick="location.href='<%=redirect_url%>';return false;">Cancel</button>
							<input type="submit" value="Save">
						</td>
					</tr>
				</tbody>
			</table>
			<p>
				<%=message%>
			</p>
		</form>
	</div>
</body>
</html>