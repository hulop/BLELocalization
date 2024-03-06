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
	response.sendRedirect("user_login.jsp?logout=true&redirect_url=" + db + "/floorplans.jsp");
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
<title>Floor Plans <%= dbname %></title>

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
<style type="text/css">
.routeTool {
	display: none;
}
.routeTool-visible {
	display: unset;
}
</style>
<script type="text/javascript" src="js/datautils.js"></script>
<script type="text/javascript" src="js/mapview.js"></script>
<script type="text/javascript" src="js/floorplan_editor.js"></script>
<script type="text/javascript" src="js/overlay.js"></script>
<script type="text/javascript" src="js/floorplans.js"></script>
<script type="text/javascript" src="js/util.js"></script>
<script type="text/javascript" src="js/commons.js"></script>
<script type="text/javascript" src="js/hokoukukan.js"></script>
</head>
<body>
	<span style="font-weight: bold"><%=profile.getString("_id")%></span>
	<a href="floorplans.jsp?logout=true">log out</a> |
	<a href="user_password.jsp?redirect_url=floorplans.jsp">change password</a> |
	<a href="../">db list</a> | 
	<span style="font-weight: bold">Manage Floor Plan</span> |
	<a href="sampling.jsp">Manage Samplings</a> |
	<a href="data_composer.jsp">Data Compose Tool</a>
	<br>

	<div id='message' style='color: red;'></div>

	<div class="floorplan_hide_edit ref_hide_edit">
		<h1 class="ui-widget-header">Floor Plans <%= dbname %></h1>
		<div style="margin-top: -10px; margin-bottom: 10px;">
			<button id="floorplanAdd_button" onclick="showFloorplanForm()">Add
				a new floorplan</button>
			<!--button id="reset_filter" onclick="resetFilter()">Reset
				filter</button-->
			<div class="fileUpload btn btn-primary">
				<span>Import floormaps.json</span>
				<input type="file" id="import_floorplans" class="upload" onchange="importFloorplans(this.files[0])"/>
			</div>
			<button id="editSelectedFloorplans" onclick="editSelectedFloorplans()">Edit selected floorplans</button>
			<button id="exportFloorplans" onclick="exportFloorplans('floormaps.zip')">Export for MapServer</button>
			<input type="text" id="findBeacon" placeholder="major-minor"></input>
			<button id="findBeaconBtn" onclick="findBeacon()">Find Beacon</button>
			<button id="findDupBeacons" onclick="findDupBeacons()">Find Duplicated Beacons</button>
			<br>
			<div class="fileUpload btn btn-primary">
				<span>Load route GeoJSON</span>
				<input type="file" id="load_route_geojson_file" class="upload" onchange="loadRouteGeoJSON(this.files[0])"/>
			</div>
			<label class="routeTool"><input type="number" id="default_width" value="5" style="width: 3em">Default Link Width (meters)</label>
			<div id="findBeaconResult"></div>
		</div>
		<div class="ui-widget-content" id="data_table"></div>
	</div>
	
	<div id="floorplan_form" class="floorplan_show_edit" style="display: none"
		title="Floor Plan">
		<form onsubmit="createFloorplan(this); return false;"
			onreset="hideFloorplanForm()">
			<input type="hidden" id="floorplan_id" name="floorplan_id" value="" />
			<p class="forCreate forEdit forImage forTile">
				<label for="name">Name:</label><br /> <input id="name" name="name"
					type="text" size="40" />
			</p>
			<p class="forCreate forEdit forImage forTile">
				<label for="comment">Comment:</label><br />
				<textarea id="comment" name="comment" cols="40" rows="5"></textarea>
			</p>
			<p class="forCreate forImage forTile">
				<input id="is_tile" name="is_tile" type="checkbox" />
				<label for="is_tile">Is this floorplan provided by tile server? </label>
			</p>
			<p class="forCreate forEdit forTile">
				<label for="tile_url">Tile URL:</label><input id="tile_url"
					name="tile_url" type="text" /><br /> 
			</p>	
			<p class="forCreate forEdit forImage">
				<input id='file' name="file" type="file" />
				<input id='filename' name='filename' type="hidden" />
			</p>
			<p class="forCreate forEdit forImage">
				<label>Type of image:<select id="type" name="type">
					<option value="floormap" selected>Floor Map</option>
					<option value="systemmap">System Map</option>
					<option value="integrated">Integrated System Map</option>
					<option value="">Others</option>
				</select></label>
			</p>
			<p class="forCreate forEdit forImage forTile forGroup">
				<label for="group">Group Name:</label><input id="group"
					name="group" type="text" />
			</p>
			<p class="forCreate forEdit forImage forTile">
				<label for="floor">Floor:</label><input id="floor"
					name="floor" type="number" />
			</p>
			<p class="forCreate forEdit forImage forGroup">
				<label for="origin_x">Origin X:</label> <input id="origin_x"
					name="origin_x" type="text" /><br /> 
				<label for="origin_y">Origin Y:</label> <input id="origin_y"
					name="origin_y" type="text" />
			</p>
			<p class="forCreate forEdit forImage forGroup">
				<label for="ppm_x">PPM X:</label><input id="ppm_x"
					name="ppm_x" type="text" /><br /> 
				<label for="ppm_y">PPM Y:</label><input id="ppm_y"
					name="ppm_y" type="text" />
			</p>			
			<p class="forCreate forEdit forImage forTile forGroup">
				<label for="lat">Anchor Latitude:</label><input id="lat"
					name="lat" type="text" /><br /> 
				<label for="lng">Anchor Longitude:</label><input id="lng"
					name="lng" type="text" /><br /> 
			</p>
			<p class="forCreate forEdit forImage forGroup">
				<label for="rotate">Anchor Rotate:</label><input id="rotate"
					name="rotate" type="text" />
			</p>
			<p class="forCreate forEdit forImage">
				<label for="z-index">z-index:</label><input id="z-index"
					name="z-index" type="text" />
			</p>
			<p class="forCreate forEdit forTile">
				<label for="coverage">Anchor Coverage:</label><input id="coverage"
					name="coverage" type="text" />
			</p>
			<p>
				<input type="submit" /> <input type="reset" value="Cancel" />
			</p>
		</form>
	</div>
	
	
	<div class="" id="floorplan_div">
		<h1 class="ui-widget-header">Floor Plan</h1>
		<div id="menu"></div>
		<div id="mapdiv"
			style="min-width: 960px; min-height: 540px; height: 98vh; height: calc(100vh - 16px); border: 1px solid black; position: relative;"></div>
	</div>
	
	
	
	<div class="">
		<h1 class="ui-widget-header">Anchor</h1>
		<div id="menu2">
		Latitude:<input type="number" id="latitude" step="0.000001" autocomplete="off"></input>
		Longitude:<input type="number" id="longitude" step="0.000001" autocomplete="off"></input>
		Rotate:<input type="number" id="anchor_rotate" step="0.1" min="-180" max="180" autocomplete="off"></input>
		Opacity:<input type="number" id="opacity" step="0.05" min="0" max="1" value="0.8" autocomplete="off"></input>
		<select multiple id="overlays"></select>
		<button id="save">Save</button>
		</div>
		<div id="mapdiv2"
			style="min-width: 960px; min-height: 540px; height: 98vh; height: calc(100vh - 16px); border: 1px solid black; position: relative;"></div>
	</div>
	
	
	
</body>
</html>
