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

var floorplanDataType = "floorplans";
var mapView, selectedFloorplan;

var osmLayer = new ol.layer.Tile({
	'source' : new ol.source.OSM({
		'wrapX' : false
	})
});

function showAnchorMap() {
	var markerLayer = new ol.layer.Vector({
		'source' : new ol.source.Vector(),
		'zIndex' : 2,
		'style' : function(feature) {
			return new ol.style.Style({
				'image' : new ol.style.Icon({
					'anchor' : [ 0.5, 1 ],
					'anchorXUnits' : 'fraction',
					'anchorYUnits' : 'fraction',
					'src' : 'images/marker.png'
				})
            });
		}
	});
	markerLayer.getSource().addFeature(regionAnchor = new ol.Feature());
	window.gmap = new ol.Map({
		'projection' : 'EPSG:3857',
		'target' : 'mapdiv2',
		'layers' : [osmLayer, markerLayer],
		'controls' : [new ol.control.ScaleLine({'units': 'metric'})],
		'view' : new ol.View({
			'center' : [0, 0],
			'zoom' : 20
		})
	});
	gmap.on('click', function (event) {
		var latLng = ol.proj.transform(event.coordinate, 'EPSG:3857', 'EPSG:4326');
		 _obj.lat = latLng[1];
		 _obj.lng = latLng[0];
		 showMapOnGlobal(_obj);
	});
  
  $("#save").button().click(function(){
	  var query = {
				'_id' : _obj._id
			}, update = {
				'$set' : {
					'lat' : parseFloat(_obj.lat),
					'lng' : parseFloat(_obj.lng),
					'rotate' : parseFloat(_obj.rotate)
				}
			};
	  
	  dataUtil.postData({
			'type' : floorplanDataType,
			'data' : {
				'action' : 'update',
				'query' : JSON.stringify(query),
				'update' : JSON.stringify(update)
			},
			'success' : function(data) {
				console.log(data);
			},
			'error' : function(xhr, text, error) {
				$('#message').text(error || text);
			}
		});
  });
  $("#is_tile").change(function() {
	  showFloorplanFormClass();
  });
  $("#latitude").on('change mouseup keyup', function() {
	  var val = parseFloat($(this).val());
	  if (_obj.lat == val) return;
	  _obj.lat = val;
	  showMapOnGlobal(_obj);
  });
  $("#longitude").on('change mouseup keyup', function() {
	  var val = parseFloat($(this).val());
	  if (_obj.lng == val) return;
	  _obj.lng = val;
	  showMapOnGlobal(_obj);
  });
  var _opacity;
  $("#opacity").on('change mouseup keyup', function() {
	  var val = parseFloat($(this).val());
	  if (_opacity == val) return;
	  _opacity = val;
	  for(var k in regionOverlays) {
		regionOverlay = regionOverlays[k];
  	    regionOverlay.setOption({opacity:val});
	  }
  });
  $("#anchor_rotate").on('change mouseup keyup', function() {
	  var val = parseFloat($(this).val());
	  if (_obj.rotate == val) return;
	  _obj.rotate = val;
	  showMapOnGlobal(_obj);
  });
  $("#overlays").change(function() {
	  for(var i = 0; i < this.options.length; i++) {
		  var o = this.options[i];
		  regionOverlays[o._data.filename || o._data.tile_url].show(o.selected);
	  }
  });
  
  // regionOverlay = new FloorPlanOverlay();
}

var regionOverlays = {};
var floorplans = [];
function showMapOnGlobal(opt) {	
	var time = (new Date()).getTime();
	opt.src = `data/file?dummy=${time}&id=${opt.filename}`;
	opt.lat = opt.lat || $("#latitude").val();
	opt.lng = opt.lng || $("#longitude").val();
	opt.rotate = opt.rotate || $("#anchor_rotate").val();
	
	var geom = new ol.geom.Point(ol.proj.transform([opt.lng, opt.lat], 'EPSG:4326', 'EPSG:3857'));
	regionAnchor.setGeometry(geom);

	$("#latitude").val(opt.lat);
	$("#longitude").val(opt.lng);
	$("#anchor_rotate").val(opt.rotate);
	$("#lat").val(opt.lat);
	$("#lng").val(opt.lng);

	var key = opt.filename || opt.tile_url
	var regionOverlay = regionOverlays[key];
	if (!regionOverlay) {
		if (opt.tile_url) {
			var tileLayer = new ol.layer.Tile({
				'source' : new ol.source.XYZ({
					'wrapX' : false,
					'url' : opt.tile_url
				})
			});
			gmap.addLayer(tileLayer);
			regionOverlay = {
				'setOption' : function(options) {
					tileLayer.setProperties(options);
				},
				'show' : function(show) {
					tileLayer.setVisible(show);
				}
			};
		} else {
			regionOverlay = new FloorPlanOverlay({}, gmap);
		}
		regionOverlays[key] = regionOverlay;
		gmap.getView().setCenter(ol.proj.transform([opt.lng, opt.lat], 'EPSG:4326', 'EPSG:3857'));
		var option = $("<option>").text(opt._metadata.name).attr("selected",true);
		$("#overlays").append(option);
		option[0]._data = opt;
	}
	regionOverlay.setOption(opt);
	regionOverlay.setOption({zIndex:1, border:1, opacity:parseFloat($("#opacity").val())});
	regionOverlay.show(true);
}

	$(document).ready(function() {
		$('button').button();
		$('.btn').button();
		refresh();
		var lastFocus;
		$('#floorplan_form').on('focusin', function(event) {
			lastFocus = event.target.id;
		});
		mapView = new MapViewer("mapdiv", {
			'editor' : FloorplanEditor,
			'mapProjection' : 'EPSG:3857',
			'externalProjection' : 'PROJ:EXTERNAL',
			'click' : function(x, y) {
				if (!$('#floorplan_form').is(':visible')) {
					return;
				}
				console.log([ x, y ]);
				if ('origin_x' == lastFocus || 'origin_y' == lastFocus) {
					var ppm_x = parseFloat($("#ppm_x").val());
					var ppm_y = parseFloat($("#ppm_y").val());
					var origin_x = parseFloat($("#origin_x").val());
					var origin_y = parseFloat($("#origin_y").val());
					$('#origin_x').val(parseInt(x * ppm_x + origin_x));
					$('#origin_y').val(parseInt(y * ppm_y + origin_y));
					var mapInfo = {
						origin : {
							x : parseInt(x * ppm_x + origin_x),
							y : parseInt(y * ppm_x + origin_y)
						},
						ppm : {
							x : ppm_x,
							y : ppm_y
						}
					};
					setProjections(mapInfo);
					mapView.drawGrid(mapInfo.origin, mapInfo.ppm);
				}
				if ('ppm_x' == lastFocus || 'ppm_y' == lastFocus) {
					var ppm_x = parseFloat($("#ppm_x").val());
					var ppm_y = parseFloat($("#ppm_y").val());
					var origin_x = parseFloat($("#origin_x").val());
					var origin_y = parseFloat($("#origin_y").val());
					if (!window._pclick) {
						window._pclick = {
							x : x,
							y : y
						};
					} else {
						var dist = Math.sqrt(Math.pow(ppm_x * (_pclick.x - x), 2) + Math.pow(ppm_y * (_pclick.y - y), 2));
						var actual = prompt("How long between distance");
						if (actual) {
							var ppm_x = parseInt(dist / actual * 100) / 100;
							var ppm_y = parseInt(dist / actual * 100) / 100;
							$("#ppm_x").val(ppm_x);
							$("#ppm_y").val(ppm_y);
							var mapInfo = {
								origin : {
									x : origin_x,
									y : origin_y
								},
								ppm : {
									x : ppm_x,
									y : ppm_y
								}
							};
							setProjections(mapInfo);
							mapView.drawGrid(mapInfo.origin, mapInfo.ppm);
						}
						_pclick = null;
					}
				}
			},
		});
		showAnchorMap();
	});

	function refresh() {
		images = {};
		refreshSampleTable();
	}

	function refreshSampleTable() {
		$('#data_table').empty();
		var query = {};

		dataUtil.getData({
			'type' : floorplanDataType,
			'data' : {
				'query' : JSON.stringify(query),
				'sort' : JSON.stringify({
					'_id' : -1
				})
			},
			'success' : function(data) {
				console.log(data);
				floorplans = data;
				createFloorplanTable(data).appendTo('#data_table').dataTable({
					'stateSave': true
				});
			},
			'error' : function(xhr, text, error) {
				$('#message').text(error || text);
			}
		});
	}

	function createFloorplan(form) {
		var $form = $(form);

		var metadata = {
			'name' : $form.find('#name').val() || '',
			'comment' : $form.find('#comment').val() || ''
		};
		var is_tile = $form.find('#is_tile')[0].checked;
		var id = $form.find('#floorplan_id').val();		
		var type = $form.find('#type').val();
		var group = $form.find('#group').val();
		var tile_url = $form.find('#tile_url').val();
		var floor = parseFloat($form.find('#floor').val());
		var origin_x = parseFloat($form.find('#origin_x').val());
		var origin_y = parseFloat($form.find('#origin_y').val());
		var ppm_x = parseFloat($form.find('#ppm_x').val());
		var ppm_y = parseFloat($form.find('#ppm_y').val());
		var lat = parseFloat($form.find('#lat').val());
		var lng = parseFloat($form.find('#lng').val());
		var rotate = parseFloat($form.find('#rotate').val());
		var coverage = parseFloat($form.find('#coverage').val());
		var filename = $form.find('#filename').val();
		var zIndex = parseFloat($form.find('#z-index').val()) || 0;
		
		if (is_tile) {
			if (!group || !tile_url || isNaN(lat) || isNaN(lng) || isNaN(coverage)) {
				alert('Invalid group, tile_url, lat, lng, or coverage');
				return;	
			}
		}
		else {
			if (isNaN(origin_x) || isNaN(origin_y) || isNaN(ppm_x) || isNaN(ppm_y) || isNaN(floor) || !group ||
				isNaN(lat) || isNaN(lng) || isNaN(rotate) || zIndex < 0) {
				alert('Invalid group, floor, origin x, origin y, ppm x, ppm y, lat, lng, rotate or z-index.');
				return;
			}
		}
		selectedFloorplan.type = type;
		selectedFloorplan.group = group;
		selectedFloorplan.floor = floor;
		selectedFloorplan.origin_x = origin_x;
		selectedFloorplan.origin_y = origin_y;
		selectedFloorplan.ppm_x = ppm_x;
		selectedFloorplan.ppm_y = ppm_y;
		selectedFloorplan.lat = lat;
		selectedFloorplan.lng = lng;
		selectedFloorplan.rotate = rotate;
		selectedFloorplan.is_tile = is_tile;
		selectedFloorplan.tile_url = tile_url;
		selectedFloorplan.coverage = coverage;
		selectedFloorplan.zIndex = zIndex;;

		if (id) { // update
			var query = {
				'_id' : objectIds ? {'$in' : objectIds} : {'$oid' : id}
			}, update, multi = objectIds ? 'true' : 'false';
			
			if (objectIds) {
				update = {
					'$set' : {
						'group' : group,
						'origin_x' : origin_x,
						'origin_y' : origin_y,
						'ppm_x' : ppm_x,
						'ppm_y' : ppm_y,
						'lat' : lat,
						'lng' : lng,
						'rotate' : rotate
					}
				};
			} else if (is_tile) {
				update = {
					'$set' : {
						'_metadata.name' : metadata.name,
						'_metadata.comment' : metadata.comment,
						'group' : group,
						'floor' : floor,
						'lat' : lat,
						'lng' : lng,
						'coverage' : coverage,
						'tile_url' : tile_url
					}
				};
			} else {
				update = {
					'$set' : {
						'_metadata.name' : metadata.name,
						'_metadata.comment' : metadata.comment,
						'type' : type,
						'group' : group,
						'floor' : floor,
						'origin_x' : origin_x,
						'origin_y' : origin_y,
						'ppm_x' : ppm_x,
						'ppm_y' : ppm_y,
						'lat' : lat,
						'lng' : lng,
						'rotate' : rotate,
						'zIndex' : zIndex
					}
				};
			}
			
			var file = $form.find("#file")[0];
			if (file.files.length > 0) {
				var reader = new FileReader();
				reader.onload = function() {
					var img = new Image();
					img.onload = function() {
						var width = img.width;
						var height = img.height;
						var formData = new FormData(form);
						dataUtil.postFormData({
							'type' : "file",
							'id' : filename,
							'data' : formData,
							'method' : 'PUT',
							'success' : function(data) {
								update["$set"].width = width;
								update["$set"].height = height;
								dataUtil.postData({
									'type' : floorplanDataType,
									'data' : {
										'action' : 'update',
										'query' : JSON.stringify(query),
										'update' : JSON.stringify(update)
									},	
									'success' : function(data) {
										console.log(data);
										deleteData("file", filename+"-thumb", null, function(data) {
											hideFloorplanForm();
											refresh();
										});
									},
									'error' : function(xhr, text, error) {
										$('#message').text(error || text);
										hideFloorplanForm();
									}
								});									
							},
							'error' : function(xhr, text, error) {
								$('#message').text(error || text);
								hideFloorplanForm();
							}
						});
					}
					img.src = reader.result;
				}
				reader.readAsDataURL(file.files[0]);
			} else { 			
				dataUtil.postData({
					'type' : floorplanDataType,
					'data' : {
						'action' : 'update',
						'query' : JSON.stringify(query),
						'update' : JSON.stringify(update),
						'multi' : multi
					},	
					'success' : function(data) {
						console.log(data);
						hideFloorplanForm();
						refresh();			
					},
					'error' : function(xhr, text, error) {
						$('#message').text(error || text);
						hideFloorplanForm();
					}
				});
			}
			
			return;
		}

		if (is_tile) {
			data = {
				'group' : group,
				'floor' : floor,
				'lat' : lat,
				'lng' : lng,
				'tile_url' : tile_url,
				'coverage' : coverage
			};
			console.log([ "send data", data ]);
			dataUtil.postData({
				'type' : floorplanDataType,
				'data' : {
					data : JSON.stringify(data),
					_metadata : JSON.stringify(metadata)
				},
				'success' : function(data) {
					console.log(data._id);
					hideFloorplanForm();
					refresh();
				},
				'error' : function(xhr, text, error) {
					$('#message').text(error || text);
				}
			});

		} else {
			var file = $form.find("#file")[0];
			if (file.files.length > 0) {
				var reader = new FileReader();
				reader.onload = function() {
					var img = new Image();
					img.onload = function() {
						var width = img.width;
						var height = img.height;
						var formData = new FormData(form);
						dataUtil.postFormData({
							'type' : "file",
							'data' : formData,
							'success' : function(data) {
								console.log([ "file save success", data ]);
								data = {
									'type' : type,
									'group' : group,
									'floor' : floor,
									'origin_x' : origin_x,
									'origin_y' : origin_y,
									'width' : width,
									'height' : height,
									'ppm_x' : ppm_x,
									'ppm_y' : ppm_y,
									'lat' : lat,
									'lng' : lng,
									'rotate' : rotate,
									'filename' : data.filename
								};
								console.log([ "send data", data ]);
								dataUtil.postData({
									'type' : floorplanDataType,
									'data' : {
										data : JSON.stringify(data),
										_metadata : JSON.stringify(metadata)
									},
									'success' : function(data) {
										console.log(data._id);
										hideFloorplanForm();
										refresh();
									},
									'error' : function(xhr, text, error) {
										$('#message').text(error || text);
									}
								});
							},
							'error' : function(xhr, text, error) {
								$('#message').text(error || text);
							}
						});
					}
					img.src = reader.result;
				}
				reader.readAsDataURL(file.files[0]);
	
				return;
			}
		}
	}

	function createFloorplanTable(data) {
		var table = $('<table>', {
			'id' : 'floorplan_table',
			'class' : 'display',
			'cellpadding' : 0,
			'cellspacing' : 0,
			'border' : 0,
			'data-order' : '[[ 0, "desc" ]]'
		});
		var head = createTableRow([ 'image', 'type', 'name', 'beacons', 'group', 'floor', 'x', 'action' ], '<th>');
		$('<thead>').append(head).appendTo(table);
		var tbody = $('<tbody>').appendTo(table);
		$('<tfoot>').append(head.clone()).appendTo(table);
		var total = 0;
		var sysmapList = {};
		data.forEach(function(obj) {
			if (obj.type != 'systemmap') return;
			var list = sysmapList[obj.floor] || [];
			list.push(obj);
			sysmapList[obj.floor] = list;
		});
		data.forEach(function(obj, i) {
			var id = obj._id.$oid;
			var info = obj.information || {};
			var md = obj._metadata || {};

			var actions = $('<span>', {
				'css' : {
					'white-space' : 'nowrap'
				}
			});

			if (info.floor) {
				console.error(info);
			}

			var select = $('<input type="checkbox" name="group_edit">').prop('obj', obj);
			// var image = new Image();

			actions.append(createButton('map', function() {
				console.log([ "showMap", obj ]);
				window._obj = obj;
				showMap(obj);
				showMapOnGlobal(obj);
			}), createButton('edit', function() {
				showFloorplanForm(obj);
			}), createButton('delete', function() {
				deleteData(floorplanDataType, id, md.name, function() {
					deleteData("file", obj.filename, null, function() {
						deleteData("file", obj.filename+"-thumb", null, function() {
							refresh();
						});
					});
				});
			}), createLink('details', `data/${floorplanDataType}/${id}`), 
			    obj.filename?createLink('image', `data/file?id=${obj.filename}`):""
			);
			if (obj.type == 'floormap' || obj.type == 'systemmap' || obj.type == 'integrated') {
				var title = obj.type == 'floormap' ? 'Create System Map' : 'Update System Map';
				var tool = $('<span>', {
					'class' : hokoukukan ? 'routeTool-visible' : 'routeTool'
				}).appendTo(actions)
				tool.append($('<br>'), createButton(title, function() {
					createSysMap(obj);
				}));
			}
			if (obj.type == 'systemmap' && sysmapList[obj.floor].length > 1) {
				actions.append($('<br>'), createButton('Create Integrated System Map', function() {
					combineMaps(sysmapList[obj.floor], sysmapList[obj.floor].indexOf(obj));
				}));
			}

			var img = obj.filename && createThumbImage(obj.filename) || null;
			var num = obj.beacons && obj.beacons.features.filter(function(a) { return a.geometry.type == "Point" }).length || 0;
			total += num;
			var type_text = obj.tile_url ? 'Tile' : $('#type [value="' + (obj.type||'') + '"]').text();
			tbody.append(createTableRow([ img, type_text, md.name, num + " beacons", obj.group, obj.floor, select, actions ], '<td>'));
		});

		tbody.append(createTableRow([ "", "", "Total", total + " beacons", "", "", "", "" ], '<td>'));
		return table;
	}

	var buttons = {}, links = {}, images = {};

	function createButton(text, onclick) {
		return (buttons[text] = buttons[text] || $('<button>', {
			'text' : text
		}).button()).clone().on('click', onclick);
	}
	var shrinking = false;
	var canvas = null;
	function shrinkImages() {
		if (shrinking) {
			return;
		}
		shrinking = true;
		for(var key in images) {
			if (!images[key].attr("src")) {
				var img = new Image();
				var time = (new Date()).getTime();
				var src = `data/file?dummy=${time}&id=${key}`;
				img.src = src+"-thumb";
				console.log("loading "+key);
				img.onerror=function() {
					img.src = src;
				}
				img.onload=function() {
					if (img.src.endsWith("thumb")) {
						images[key].attr("src", img.src);
						shrinking = false;
						shrinkImages();
						return;
					}
					if (!canvas) {
						canvas = $('<canvas>');
					}
					var height = 300;
					var scale = height / img.height;
					var width = img.width * scale;
					canvas[0].width = width;
					canvas[0].height = height;
					var ctx = canvas[0].getContext("2d");
					ctx.drawImage(img, 0, 0, img.width, img.height, 0, 0, width, height);
					images[key].attr("src", canvas[0].toDataURL());	
					shrinking = false;
					shrinkImages();					
					saveImage(key+"-thumb", canvas[0].toDataURL());
				}
				return;
			}
		}
		shrinking = false;
	}
	function createImage(src) {
		$img = $('<img>', {
			'src' : src,
			'height' : '60px'
		});
		images[src] = $img;
		return $img;
	}
	function createThumbImage(id) {
		$img = $('<img>', {
			'src' : '',
			'height' : '60px'
		});
		images[id] = $img;
		shrinkImages()
		return $img;
	}
	function saveImage(id, dataurl) {
		var bin = atob(dataurl.replace(/^.*,/, ''));
	    var buffer = new Uint8Array(bin.length);
	    for (var i = 0; i < bin.length; i++) {
	        buffer[i] = bin.charCodeAt(i);
	    }
	    var blob = new Blob([buffer.buffer], {
	        type: "image/png"
	    });
	    var formData = new FormData();
	    formData.append("file",blob);
	    dataUtil.postFormData({
			'type' : "file",
			'id' : id,
			'data' : formData,
			'method' : 'PUT',
			'success' : function(data) {
			},
			'error' : function(xhr, text, error) {
				$('#message').text(error || text);
			}
		});
	}

	function createLink(text, href) {
		return (links[text] = links[text] || $('<a>', {
			'text' : text,
			'target' : '_blank'
		}).button()).clone().attr('href', href);
	}

	function createTableRow(array, tag) {
		var row = $('<tr>');
		array.forEach(function(col) {
			row.append($(tag).append(col));
		});
		// row.find('button,a').button();
		return row;
	}

	function deleteData(type, id, name, success) {
		if (!name || confirm('Delete ' + (name || id) + '?')) {
			dataUtil.deleteData({
				'type' : type,
				'id' : id,
				'success' : success,
				'error' : function(xhr, text, error) {
					$('#message').text(error || text);
				}
			});
		}
	}

	var currentObj = null;
	function showFloorplanFormClass() {
		$('#floorplan_form .forCreate, #floorplan_form .forEdit').hide();
		if (currentObj) {
			if (currentObj.tile_url) {
				$('#floorplan_form .forEdit.forTile').show();
			} else {
				$('#floorplan_form .forEdit.forImage').show();
			}
		} else {
			if ($('#is_tile')[0].checked) {
				$('#floorplan_form .forCreate.forTile').show();
			} else {
				$('#floorplan_form .forCreate.forImage').show();
			}
		}
	}
	function showFloorplanForm(obj, ref) {
		objectIds = null;
		$('.floorplan_hide_edit').hide();

		currentObj = obj;

		$('#floorplan_form').dialog({
			'width' : 'auto',
			'height' : 'auto',
			'resizable' : false,
			'close' : function() {
				hideFloorplanForm();
			}
		});

		console.log(obj);
		if (obj) {
			showMap(obj);
		}
		selectedFloorplan = obj = obj || {};

		var md = obj._metadata || {};
		var form = $('#floorplan_form form');
		var info = obj || {};
		form.find('#name').val(md.name || '');
		form.find('#comment').val(md.comment || '');
		form.find('#is_tile')[0].checked = obj.tile_url || false; 
		form.find('#tile_url').val(obj.tile_url || '');	
		form.find('#file').val('');
		form.find('#filename').val(obj.filename || '');
		form.find('#floorplan_id').val((obj._id && obj._id.$oid) || '');
		form.find('#group').val(obj.group || "");
		form.find('#floor').val(obj.floor || 0);
		form.find('#origin_x').val(obj.origin_x || 0);
		form.find('#origin_y').val(obj.origin_y || 0);
		form.find('#ppm_x').val(obj.ppm_x || 1);
		form.find('#ppm_y').val(obj.ppm_y || 1);
		form.find('#lat').val(obj.lat || 0);
		form.find('#lng').val(obj.lng || 0);
		form.find('#rotate').val(obj.rotate || 0);
		form.find('#coverage').val(obj.coverage || 0);
		form.find('#type').val(obj.type || '');
		form.find('#z-index').val(obj.zIndex || 0);

		showFloorplanFormClass();
	}

	/*
	  importAttachments will read files from a directory
	  if floorplans.json file is found, it will try to upload all the floorplans including images
	*/
	function importAttachments(files){
		var imageFileMap = {};
		function processFloorplan(floorplan, callback) {
			console.log(floorplan);
			var imageName = floorplan["image"].split("/").pop();
			var imageFile = imageFileMap[imageName];
			if (imageFile == null) {
				console.log(`could not find ${imageName}`);
			}
			var id = floorplan["id"];

			var metadata = {
				'name': id,
				'comment': 'imported from attachments',
			};

			var uploadData = {
				'type' : 'floormap',
				'group' : 'attachments',
				'floor' : floorplan.floor,
				'origin_x' : floorplan.origin_x,
				'origin_y' : floorplan.origin_y,
				'ppm_x' : floorplan.ppm_x,
				'ppm_y' : floorplan.ppm_y,
				'lat' : floorplan.lat,
				'lng' : floorplan.lng,
				'rotate' : floorplan.rotate,
				'zIndex' : floorplan.zIndex,
			};
			
			var reader = new FileReader();
			reader.onload = function () {
				var img = new Image();
				img.onload = function () {
					var width = img.width;
					var height = img.height;
					var formData = new FormData();
					formData.append('file', imageFile, imageFile.name);
					console.log(formData);
					dataUtil.postFormData({
						'type': "file",
						'data': formData,
						'method': 'POST',
						'success': function (data) {
							console.log(data);
							uploadData.width = width;
							uploadData.height = height;
							uploadData.filename = data.filename
							console.log(uploadData);
							dataUtil.postData({
								'type': floorplanDataType,
								'data': {
									'_metadata': JSON.stringify(metadata),
									'data': JSON.stringify(uploadData)
								},
								'success': function (data) {
									console.log(data);
									callback();
								},
								'error': function (xhr, text, error) {
									$('#message').text(error || text);
								}
							});
						},
						'error': function (xhr, text, error) {
							$('#message').text(error || text);
						}
					});
				}
				img.src = reader.result;
			}
			reader.readAsDataURL(imageFile);
		}

		for (var i = 0; i < files.length; i++) {
			var file = files[i];
			var path = file.webkitRelativePath || file.name;

			if (path.includes("floormaps.json")) {
				console.log(`found ${path}`)
				var fr = new FileReader()
				fr.onload = () => {
					var floorplans = JSON.parse(fr.result);
					var count = 0;
					for (var j = 0; j < floorplans.length; j++) {
						processFloorplan(floorplans[j], () => {
							count++;
							if (count == floorplans.length) {
								refresh();
							}
						});
					}
					
				}
				fr.readAsText(file);
			} else {
				filename = path.split('/').pop();
				imageFileMap[filename] = file;
			}
		}
	}

	function showMap(obj) {
		
		if (obj.filename) {		
			var image = new Image();
			var time = (new Date()).getTime();
			image.src = `data/file?dummy=${time}&id=${obj.filename}`;
			image.onload = function() {
	
				var mapInfo = {
					imageURL : image.src,
					width : image.width,
					height : image.height,
					origin : {
						x : obj.origin_x,
						y : obj.origin_y
					},
					ppm : {
						x : obj.ppm_x,
						y : obj.ppm_y
					}
				};
				mapView.show(mapInfo);
				setProjections(mapInfo);
				mapView.drawGrid(mapInfo.origin, mapInfo.ppm);
	
				mapView.getEditor().showBeaconsInFloorplan(obj);
			};
			$('#floorplan_div').show();
			$('#anchor_rotate').prop('disabled', false);
		}
		if (obj.tile_url) {
			// TODO show map, beacons
			$('#floorplan_div').hide();
			$('#anchor_rotate').prop('disabled', true);
		}
	}

	var mapInfo;
	function setProjections(_mapInfo) {
		mapInfo = _mapInfo;
		var projection = new ol.proj.Projection({
			'code' : 'PROJ:EXTERNAL'
		});
		ol.proj.addProjection(projection);
		ol.proj.addCoordinateTransforms('EPSG:3857', projection, function(coordinate) {
			var x = coordinate[0], y = coordinate[1];
			return [ (x - mapInfo.origin.x) / mapInfo.ppm.x, (y - mapInfo.origin.y) / mapInfo.ppm.y ];
		}, function(coordinate) {
			var x = coordinate[0], y = coordinate[1];
			return [ x * mapInfo.ppm.x + mapInfo.origin.x, y * mapInfo.ppm.y + mapInfo.origin.y ];
		});
	}

	function hideFloorplanForm() {
		if ($('#floorplan_form').is(':visible')) {
			$('#floorplan_form').dialog('close');
		}
		$('.floorplan_hide_edit').show();
		if (selectedFloorplan) {
			showMap(selectedFloorplan);
		}
	}
	
	function exportFloorplans(filename) {
		filename = filename || 'floormaps.json';
		function normalizelat(deg) {
			while(deg < -180) {
				deg += 360;
			}
			while(deg > 180) {
				deg -= 360;
			}
			return deg;
		}
		console.log(floorplans);
		var temp = [];
		var idCount = {};
		floorplans.forEach(function(fp) {
			if (fp.tile_url) {
				console.log(fp);
				temp.push({
					'coverage': fp.coverage,
					'floor': fp.floor<0?fp.floor:fp.floor+1,
					'tile_url': fp.tile_url,
					'attributions': "<replace with your attributions>",
					'lat': fp.lat-0,
					'lng': normalizelat(fp.lng-0),
					'id': fp._metadata.name+"-"+fp._id.$oid
				});
				return;
			}
			if (fp.type != 'floormap') {
				return;
			}
			console.log(fp);
			var id = fp._metadata.name;
			if (idCount[id]) {
				id += ++idCount[id];
			} else {
				idCount[id] = 1;
			}
			temp.push({
				rotate: fp.rotate,
				floor: fp.floor<0?fp.floor:fp.floor+1,
				image: "<replace with your path>/"+fp.filename,
				height: fp.height,
				width: fp.width,
				ppm_y: fp.ppm_x,
				ppm_x: fp.ppm_y,
				origin_y: fp.origin_y,// || fp.height/2,
				origin_x: fp.origin_x,// || fp.width/2,
				lat: fp.lat-0,
				lng: normalizelat(fp.lng-0),
				id: id,
				zIndex: fp.zIndex || 0
			});
		});
		if (filename.endsWith('.json')) {
			downloadFile(JSON.stringify(temp), "json", filename);
			return;
		}
		var json = [];
		(function load(list) {
			var fp = list.shift();
			if (fp) {
				json.push(fp)
				var m = /^(.*)\/(.*)$/.exec(fp.image);
				if (m) {
					toDataURL('data/file?' + $.param({
						'dummy' : new Date().getTime(),
						'id': m[2]
					}), function(url) {
						fp.image = url;
						load(list);
					});
				} else {
					load(list);
				}
			} else {
				var actionURL = 'data?' + $.param({
					'dummy' : new Date().getTime(),
					'export' : 'floormaps.json',
					'filename' : filename
				});
				$('form#floormaps_form').remove();
				var form = $('<form>', {
					'id' : 'floormaps_form',
					'method' : 'post',
					'action' : actionURL
				}).append($('<input>', {
					'type' : 'hidden',
					'name' : 'floormaps',
					'value' : JSON.stringify(json)
				})).appendTo($('body')).submit();
			}
		})(temp);
	}
	function downloadFile(data, type, filename) {  
		var mime = "text/plain";
		if (type == "json") mime = 'text/json;charset=utf-8;';
		if (type == "csv") mime = 'text/comma-separated-values';
		
		  var blob = new Blob([data], { type: mime });
		  if (navigator.msSaveBlob) {
		    navigator.msSaveBlob(blob, filename);
		  } else {
		    var link = document.createElement("a");
		    if (link.download !== undefined) {
		      var url = URL.createObjectURL(blob);
		      link.setAttribute("href", url);
		      link.setAttribute("download", filename);
		    } else {        
		      link.href = `data:attachment/${type},` + data;
		    }
		    link.style = "visibility:hidden";
		    document.body.appendChild(link);
		    link.click();
		    document.body.removeChild(link);
		  }
		}
	
	function findBeacon() {
		var id = $("#findBeacon").val();
		var major = parseInt(id.split("-")[0]);
		var minor = parseInt(id.split("-")[1]);
		var str = "";		
		floorplans.forEach(function(fp) {			
			if (fp.beacons) {
				fp.beacons.features.forEach(function(f) {
					if (f.properties && f.properties.type == "beacon") {
						if (f.properties.major == major && f.properties.minor == minor) {
							str += `${fp.group}-${fp.floor>=0?(fp.floor+1)+"F":"B"+(-fp.floor)+"F"}: `;
							str += `(${f.geometry.coordinates[0]}, ${f.geometry.coordinates[1]})\n`;
						} 
					}
				});
			}
		});
		$("#findBeaconResult").html("<pre>"+str+"</pre>");
	}
	function findDupBeacons() {
		// var beacons = [];
		var beaconMap = {};
		floorplans.forEach(function(fp) {			
			if (fp.beacons) {
				fp.beacons.features.forEach(function(f) {
					if (f.properties && f.properties.type == "beacon") {
						var beacon = {
							uuid: f.properties.uuid,
							major: f.properties.major,
							minor: f.properties.minor,
							x: f.geometry.coordinates[0],
							y: f.geometry.coordinates[1],
							floorplan: `${fp.group}-${fp.floor>=0?(fp.floor+1)+"F":"B"+(-fp.floor)+"F"}`
						};
						// beacons.push(beacon);
						var key = beacon.major+"-"+beacon.minor;
						var temp = beaconMap[key];
						if (!temp) {
							temp = [];
							beaconMap[key] = temp;
						}
						temp.push(beacon);
					}
				});
			}
		});
		var str = "";
		for(var k in beaconMap) {
			var beacons = beaconMap[k];
			if (beacons.length > 1) {
				beacons.forEach(function(b) {
					str += `${b.floorplan}[${b.major}-${b.minor}]: (${b.x}, ${b.y})\n`;
				});
			}
		}
		$("#findBeaconResult").html("<pre>"+str+"</pre>");
	}

	function combineMaps(maps, anchorIndex) {
		maps = [].concat(maps);
		var images = [];
		(function loadMaps(map) {
			if (map) {
				var img = $('<img>', {
					'src' : 'data/file?' + $.param({
						'dummy' : new Date().getTime(),
						'id': map.filename
					}),
					'on' : {
						'load' : function(event) {
							images.push({
								'map' : map,
								'img' : img[0]
							});
							loadMaps(maps.shift());
						}
					}
				});
			} else {
				combineImages(images);
			}
		})(maps.splice(anchorIndex, 1)[0]);
	}

	function combineImages(images) {
		var map_base = images[0].map;
		var anchor_base;
		var min_x = Infinity, min_y = Infinity, max_x = -Infinity, max_y = -Infinity;
		var name = '';
		images.forEach(function(image, index) {
			var map = image.map;
			if (name.length > 0) {
				name += ', ';
			}
			name += map._metadata.name
			var left = -map.origin_x / map.ppm_x;
			var right = (map.width - map.origin_x) / map.ppm_x;
			var bottom = -map.origin_y / map.ppm_y;
			var top = (map.height - map.origin_y) / map.ppm_y;
			var anchor = {
				'lat' : map.lat,
				'lon' : map.lng,
				'rotate' : map.rotate
			};
			anchor_base = anchor_base || anchor;
			function reproject(x, y) {
				var xy = latlon2xy(xy2latlon({'x' : x, 'y' : y}, anchor), anchor_base);
				min_x = Math.min(min_x, xy.x);
				min_y = Math.min(min_y, xy.y);
				max_x = Math.max(max_x, xy.x);
				max_y = Math.max(max_y, xy.y);
				return xy;
			}
			reproject(left, bottom);
			reproject(left, top);
			reproject(right, bottom);
			reproject(right, top);
		});
// console.log(min_x, min_y, max_x, max_y);
		var ppm_x = Math.max(10, map_base.ppm_x), ppm_y = Math.max(10, map_base.ppm_y);
		var width = Math.ceil((max_x - min_x) * ppm_x);
		var height = Math.ceil((max_y - min_y) * ppm_y);
		var canvas = $('<canvas>')[0];
		canvas.width = width;
		canvas.height = height;
		var ctx = canvas.getContext('2d');
		ctx.fillStyle = 'black';
		ctx.fillRect(0, 0, width, height);
		ctx.scale(ppm_x, ppm_y);
		ctx.translate(-min_x, height / ppm_y + min_y);
		images.forEach(function(image, index) {
			var map = image.map;
			var rotate = map.rotate - map_base.rotate;
			var local = latlon2xy({'lat': map.lat, 'lon': map.lng}, anchor_base);
			ctx.save();
			ctx.translate(local.x, -local.y);
			ctx.rotate(rotate / 180 * Math.PI);
			ctx.translate(-map.origin_x / map.ppm_x, (map.origin_y - map.height) / map.ppm_y);
			ctx.drawImage(image.img, 0, 0, map.width / map.ppm_x, map.height / map.ppm_y);
			ctx.restore();
		});
		var fpData = {
			'type' : 'integrated',
			'group' : map_base.group,
			'floor' : map_base.floor,
			'origin_x' : -min_x * ppm_x,
			'origin_y' : -min_y * ppm_y,
			'width' : width,
			'height' : height,
			'ppm_x' : ppm_x,
			'ppm_y' : ppm_y,
			'lat' : map_base.lat,
			'lng' : map_base.lng,
			'rotate' : map_base.rotate
		}, metadata = {
			'name' : 'integrated ' + name,
			'comment' : 'This system map is automatically generated.'
		};
		putCanvas(canvas, fpData, metadata)
	}

	function putCanvas(canvas, fpData, metadata) {
		saveCanvas(canvas, {'success' : function(data) {
			fpData.filename = data.filename;
			dataUtil.postData({
				'type' : floorplanDataType,
				'data' : {
					'data' : JSON.stringify(fpData),
					'_metadata' : JSON.stringify(metadata)
				},
				'success' : refresh,
				'error' : function(xhr, text, error) {
					$('#message').text(error || text);
				}
			});
		}});
	}

	function saveCanvas(canvas, options) {
		if (canvas.toBlob) {
			canvas.toBlob(function(blob) {
			    var formData = new FormData();
			    formData.append('file', blob);
			    dataUtil.postFormData({
					'type' : 'file',
					'id': options.id,
					'data' : formData,
					'method' : 'PUT',
					'success' : options.success,
					'error' : function(xhr, text, error) {
						$('#message').text(error || text);
					}
				});
			}, 'image/png');
		}  else {
			window.open(canvas.toDataURL('image/png'), 'debug');
		}
	}

	function toDataURL(url, callback) {
		$commons.loadBlob(url, function(blob) {
			$commons.blobToDataURL(blob, callback);
		});
	}

	var hokoukukan;
	function loadRouteGeoJSON(file) {
		if (file) {
			var fr = new FileReader();
			fr.onload = function() {
				var geojson = JSON.parse(fr.result);
				console.log(geojson);
				hokoukukan = $hokoukukan(geojson);
				$('.routeTool').addClass('routeTool-visible');
			};
			fr.readAsText(file);
		}
	}

	function createSysMap(obj) {
		if (!hokoukukan) return;
		console.log(obj);
		var layer = {
			'floor_num' : obj.floor
		};
		var anchor = {
			'lat' : obj.lat,
			'lon' : obj.lng,
			'rotate' : obj.rotate
		};
		var width = obj.width, height = obj.height;
		var canvas = $('<canvas>')[0];
		canvas.width = width;
		canvas.height = height;
		var ctx = canvas.getContext('2d');
//		ctx.fillStyle = 'white';
//		ctx.fillRect(0, 0, width, height);
		ctx.translate(obj.origin_x, height - obj.origin_y);
		ctx.scale(obj.ppm_x, -obj.ppm_y);
		hokoukukan.drawBack(ctx, layer, anchor);
		hokoukukan.drawLinks(ctx, layer, anchor);
		if (obj.type != 'floormap') {
			saveCanvas(canvas, {
				'id' : obj.filename,
				'success' : function(data) {
					console.log(data);
					deleteData("file", obj.filename+"-thumb", null, function() {
						refresh();
					});
				}
			});
			return;
		}
		var fpData = {
			'type' : 'systemmap',
			'group' : obj.group,
			'floor' : obj.floor,
			'origin_x' : obj.origin_x,
			'origin_y' : obj.origin_y,
			'width' : obj.width,
			'height' : obj.height,
			'ppm_x' : obj.ppm_x,
			'ppm_y' : obj.ppm_y,
			'lat' : obj.lat,
			'lng' : obj.lng,
			'rotate' : obj.rotate
		}, metadata = {
			'name' : 'system map ' + obj._metadata.name,
			'comment' : 'This system map is automatically generated.'
		};
		putCanvas(canvas, fpData, metadata)
	}

	var objectIds;
	function editSelectedFloorplans() {
		var array = $('input[name="group_edit"]:checked').map(function(i, e) {
			return e.obj;
		}).get().filter(function(obj) {
			return !obj.tile_url;
		});
		if (array.length > 1) {
			console.log(array);
			showFloorplanForm(array[0]);
			objectIds = array.map(function(obj) {
				return obj._id;
			});
			$('#floorplan_form .forEdit').hide();
			$('#floorplan_form .forGroup').show();
		} else {
			alert('Select more than one floorplan');
		}
	}
