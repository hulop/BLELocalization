/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation
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

function DefaultMapView(div, options) {
	options = options || {};
	var MAP_BASE = "./data", mapData = null, allBeacons = null;
	var floors = [];
	var viewer = new MapViewer(div, {
		'click' : options.click,
		'featureclick' : options.featureclick,
		'enableEdit' : options.enableEdit,
		'mapProjection' : 'EPSG:3857',
		'externalProjection' : 'PROJ:EXTERNAL',
		'mousePositionFormatOutput' : function(lonLat) {
			return (lonLat.lon).toFixed(2) + 'm, ' + (lonLat.lat).toFixed(2) + 'm';
		}
	});
	var projection = new ol.proj.Projection({
		'code' : 'PROJ:EXTERNAL'
	});
	ol.proj.addProjection(projection);
	ol.proj.addCoordinateTransforms('EPSG:3857', projection, function(coordinate) {
		var mapInfo = viewer && viewer.getMapInfo();
		if (!mapInfo) {
			return coordinate;
		}
		var x = coordinate[0], y = coordinate[1];
		return [ (x - mapInfo.origin_x) / mapInfo.ppm_x, (y - mapInfo.origin_y) / mapInfo.ppm_y ];
	}, function(coordinate) {
		var mapInfo = viewer && viewer.getMapInfo();
		if (!mapInfo) {
			return coordinate;
		}
		var x = coordinate[0], y = coordinate[1];
		return [ x * mapInfo.ppm_x + mapInfo.origin_x, y * mapInfo.ppm_y + mapInfo.origin_y ];
	});
	
	
	
	loadData();

	function showFloorList(floors) {
		console.log('showFloorList');
		var select = $('<select id="site" size="5">');
		$('#menu').empty();
		floors.forEach(function(floor) {
			console.log(floor);
			select.append($('<option>', {
				'text' : floor.label,
				'click' : function() {
					showFloor(floor.floorInfo);
				}
			}));
		});
		$('#menu').append(select);
		if (options && 'floors_ready' in options) {
			options.floors_ready();
		}
	}

	function showFloor(floor, options) {
		console.log('showFloor');

		floor.imageURL = MAP_BASE + '/file?id='+floor.filename;
		viewer.show(floor);
		viewer.drawGrid({
			x:floor.origin_x,
			y:floor.origin_y
		},
		{
			x:floor.ppm_x,
			y:floor.ppm_y
		});

		//viewer.drawBeacon(getBeacons(floor.floor));
		if (options && 'success' in options) {
			options.success();
		}
	}

	function loadData() {
		$.ajax({
			'type' : 'GET',
			'url' : MAP_BASE + '/floorplans',
			'success' : function(data) {
				console.log(data);
				mapData = data;
				mapData.forEach(function(floor){
					floor.floor_num = floor.floor;
					floor.floor = getFloorStr(floor);
				});
				showFloorList(getFloorList());
			},
			'error' : function() {
				console.log('error: ');
			}
		});
	}

	var markers = [];
	function addMarker(marker) {
		markers.push(marker);
		viewer.drawMarker(markers);
	}

	function addMarkers(markerArray) {
		viewer.drawMarker(markers = markers.concat(markerArray));
	}

	function clearMarkers() {
		viewer.drawMarker(markers = []);
	}
	function getFloorStr(floor) {
		var n = floor.floor;
		
		if (n-0 >= 0) {
			return floor.group+"-"+(n-0+1)+"F";
		}
		if (n-0 < 0) {
			return floor.group+"-B"+(-n)+"F";
		}
	}

	function getFloorList() {
		var floors = [];
		if (mapData) {
			mapData.forEach(function(floor) {
				floors.push({
					'label' : floor._metadata.name,
					'floorInfo' : floor
				});
			});
		}
		return floors;
	}

	return {
		'getViewer' : function() {
			return viewer;
		},
		'getFloorList' : getFloorList,
		'loadFloor' : showFloor,
		'addMarker' : addMarker,
		'addMarkers' : addMarkers,
		'clearMarkers' : clearMarkers
	};
}
