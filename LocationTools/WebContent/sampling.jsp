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
	response.sendRedirect("user_login.jsp?logout=true&redirect_url=" + db + "/sampling.jsp");
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
<link rel="stylesheet" type="text/css"
	href="js/lib/jquery-ui-1.11.4/jquery-ui.min.css">
<script type="text/javascript"
	src="js/lib/jquery-ui-1.11.4/jquery-ui.min.js"></script>
<link rel="stylesheet" type="text/css"
	href="js/lib/DataTables-1.10.10/media/css/jquery.dataTables.css">
<script type="text/javascript"
	src="js/lib/DataTables-1.10.10/media/js/jquery.dataTables.min.js"></script>
<link rel="stylesheet" type="text/css"
	href="js/lib/OpenLayers-4.6.4/ol.css">
<script type="text/javascript"
	src="js/lib/OpenLayers-4.6.4/ol.js"></script>
<link rel="stylesheet" type="text/css" href="css/common.css">
<script type="text/javascript" src="js/datautils.js"></script>
<script type="text/javascript" src="js/mapview.js"></script>
<script type="text/javascript" src="./js/defaultMap.js"></script>

<script type="text/javascript" src="js/commons.js"></script>
<script type="text/javascript" src="js/lidar_import.js"></script>
<script type="text/javascript" src="js/sampling.js"></script>
</head>
<body>
	<span style="font-weight: bold"><%=profile.getString("_id")%></span>
	<a href="sampling.jsp?logout=true">log out</a> |
	<a href="user_password.jsp?redirect_url=sampling.jsp">change password</a> |
	<a href="../">db list</a> | 
	<a href="floorplans.jsp">Manage Floor Plan</a> |
	<span style="font-weight: bold">Manage Samplings</span> |
	<a href="data_composer.jsp">Data Compose Tool</a>
	<br>
	<div class="fileUpload btn btn-primary" style="position: absolute; right: 8px; top: 4px;">
		<span onclick="$lidar_import.showDialog()">import LiDAR samplings</span>
	</div>

	<div id='message' style='color: red;'></div>

	<div id="refpoint_form" class="ref_show_edit" style="display: none"
		title="Reference point">
		<form onsubmit="createRefData(this); return false;"
			onreset="hideRefForm()">
			<input type="hidden" id="ref_id" name="ref_id" value="" />
			<p>
				<label for="ref_name">Name:</label><br /> <input id="ref_name"
					name="ref_name" type="text" size="40" />
			</p>
			<p>
				<label for="ref_comment">Comment:</label><br />
				<textarea id="ref_comment" name="ref_comment" cols="40" rows="5"></textarea>
			</p>
			<p>
				<label for="ref_floor">Floor:</label><br /> <select id="ref_floor"
					name="ref_floor"></select>
			</p>
			<p>
				<label for="ref_x">X:</label><br /> <input id="ref_x" name="ref_x"
					type="text" />
			</p>
			<p>
				<label for="ref_y">Y:</label><br /> <input id="ref_y" name="ref_y"
					type="text" />
				<button
					onclick="event.preventDefault(); showFormMarker($('#ref_x').val(), $('#ref_y').val()); return false;">Map</button>
			</p>
			<p>
				<label for="ref_rotate">Rotate:</label><br /> <input id="ref_rotate" name="ref_rotate"
					type="text" />
			</p>
			<p>
				<input type="submit" /> <input type="reset" value="Cancel" />
			</p>
		</form>
	</div>
	<div class="ref_hide_edit sample_hide_edit">
		<h1 class="ui-widget-header">Reference points <%= dbname %></h1>
		<div style="margin-top: -10px; margin-bottom: 10px;">
			<button id="refAdd_button" onclick="showRefForm()">Add a
				reference point</button>
			<button id="export_button" onclick="exportSamplings()">Export
				selected samples</button>
			<button id="export_csv_button" onclick="exportCSVSamplings()">Export
				selected samples (CSV)</button>
			<button id="plot_button" onclick="plotSamplings()">Plot
				selected samples</button>
			<button id="plot_button" onclick="deleteRefpoints()">Delete
				selected refpoints</button>
			<button id="export_all_button" onclick="exportAllSamplings()">Export
				all samples</button>
			<button id="export_all_csv_button" onclick="exportAllSamplingsCSV()">Export
				all samples (CSV)</button>
			<button id="export_all_csv_button" onclick="$('#ref_data_table input[type=checkbox]').prop('checked', true)">Select
				all</button>
			<button id="export_all_csv_button" onclick="$('#ref_data_table input[type=checkbox]').prop('checked', false)">Unselect
				all</button><br>
				
			<label for="ref_floor2">Floor:</label><select id="ref_floor2"
					name="ref_floor"></select>
			<div class="fileUpload btn btn-primary">
    			<span>Import LiDAR Sampling</span>
    			<input type="file" id="import_lidar_file" class="upload" onchange="importLidarSamplings()"/>
			</div>

			<div class="fileUpload btn btn-primary">
    			<span>Import JSON Sampling</span>
    			<input type="file" id="import_json_sample_file" class="upload" onchange="importJSONSamplings()"/>
			</div>

		</div>
		<div class="ui-widget-content" id="ref_data_table"></div>
	</div>

	<div id="sampling_form" class="sample_show_edit" style="display: none"
		title="Sampling data">
		<form onsubmit="createSampleData(this); return false;"
			onreset="hideSampleForm()">
			<input type="hidden" id="sample_id" name="sample_id" value="" />
			<p>
				<label for="name">Name:</label><br /> <input id="name" name="name"
					type="text" size="40" />
			</p>
			<p>
				<label for="comment">Comment:</label><br />
				<textarea id="comment" name="comment" cols="40" rows="5"></textarea>
			</p>
			<p class="forCreate">
				<input id='data' name="data" type="file" />
			</p>
			<p class="forEdit">
				<label for="sample_tags">Tags:</label><br />
				<textarea id="sample_tags" name="sample_tags" cols="30" rows="5"></textarea>
			</p>
			<p class="forEdit">
				<label for="sample_x">X:</label><br /> <input id="sample_x"
					name="sample_x" type="text" />
			</p>
			<p class="forEdit">
				<label for="sample_y">Y:</label><br /> <input id="sample_y"
					name="sample_y" type="text" />
				<button id="sample_map">Map</button>

			</p>
			<p class="forEdit">
				<label for="sample_z">Z:</label><br /> <input id="sample_z"
					name="sample_z" type="text" />
			</p>
			<p>
				<input type="submit" /> <input type="reset" value="Cancel" />
			</p>
		</form>
	</div>
	<div class="sample_hide_edit ref_hide_edit">
		<h1 class="ui-widget-header">Sampling data <%= dbname %></h1>
		<div style="margin-top: -10px; margin-bottom: 10px;">
			<button id="sampleAdd_button" onclick="showSampleForm()">Add
				a sampling data</button>
			<button id="reset_filter" onclick="resetFilter()">Reset
				filter</button>
		</div>
		<div class="ui-widget-content" id="data_table"></div>
	</div>

	<div class="">
		<h1 class="ui-widget-header">Floor Plan</h1>
		<div id="menu" style="display: none"></div>
		<div id="mapdiv"
			style="min-width: 960px; min-height: 540px; height: 98vh; height: calc(100vh - 16px); border: 1px solid black; position: relative;"></div>
	</div>

	<div id="lidar_import_dialog" style="display: none" title="Import LiDAR Samplings">
		<p>
			<input id="li_file" type="file" accept=".json" />
		</p>
		<p>
			<label>Group Name:<input id="li_group" type="text" /></label>
		</p>
		<p>
			<label>Anchor Latitude:<input id="li_lat" type="text" /></label><br />
			<label>Anchor Longitude:<input id="li_lng" type="text" /></label><br />
		</p>
		<p>
			<button id="li_submit">Import</button>
			<button id="li_cancel">Cancel</button>
		</p>
	</div>
</body>
</html>
