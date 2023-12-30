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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

function FloorplanEditor(map, options) {
	var editor = this;
	var baseURL = options.baseURL;
	var clickControl = options.clickControl;
	var mapProjection = options.mapProjection;
	var editorProjection = options.editorProjection;
	var floorplanDataType = "floorplans";
	var format_geojson = new ol.format.GeoJSON();

	function getStyle(feature) {
		var label = feature.get('label') || '';;
		switch (feature.get('type')) {
			case 'beacon':
				var uuid = (feature.get('uuid') || '').substr(-6);
				var major = feature.get('major') || '';
				var minor = feature.get('minor') || '';
				label = uuid + '\n' + major + "-" + minor;
				break;
			case 'accessibility':
				label = "A";
				break;
		}
		return new ol.style.Style({
	        'fill': new ol.style.Fill({
	            'color': 'rgba(238, 153, 0, 0.3)'
	        }),
	        'stroke': new ol.style.Stroke({
	            'color': 'rgba(238, 153, 0, 1)',
	            'width': 1
	        }),
	        'image': new ol.style.Circle({
	            'radius': 8,
	            'fill': new ol.style.Fill({
	                'color': 'rgba(238, 153, 0, 0.3)',
	            }),
		        'stroke': new ol.style.Stroke({
		            'color': 'rgba(238, 153, 0, 1)',
		            'width': 1
		        }),
	        }),
			'text' : new ol.style.Text({
				'textAlign' : 'center',
				'textBaseline' : 'middle',
				'font' : 'bold 12px Helvetica',
				'text' : label,
				'fill' : new ol.style.Fill({
					'color' : 'rgba(0, 0, 0, 0.5)'
				}),
				'stroke' : new ol.style.Stroke({
					'color' : 'rgba(255, 255, 255, 0.5)',
					'width' : 3
				})
			})
		});
	}
	var drawLayer = new ol.layer.Vector({
		'source' : new ol.source.Vector(),
		'zIndex' : 2,
		'style' : function(feature) {
			return getStyle(feature);
		}
	});

	map.addLayer(drawLayer);

	var drawingInteractions = {
		'select' : {
			'label' : 'Select',
			'add' : function (map) {
				var interaction = new ol.interaction.Select({
					'layers' : [drawLayer]
				});
				interaction.set('_mine', true);
				interaction.on('select', function(event) {
					console.log(event.selected);
					showForm(event.selected);
				});
				map.addInteraction(interaction);
				interaction = new ol.interaction.DragBox({
					'layers' : [drawLayer]
				});
				interaction.set('_mine', true);
				interaction.on('boxend', function(event) {
					var extent = interaction.getGeometry().getExtent();
					var selected = [];
					drawLayer.getSource().forEachFeatureIntersectingExtent(extent, function(feature) {
						feature.getGeometry().getType() == 'Point' && selected.push(feature);
			        });
					showForm(selected);
				});
				map.addInteraction(interaction);
			}
		},
		'beacon' : {
			'label' : 'Add',
			'add' : function (map) {
				var interaction = new ol.interaction.Draw({
                    'type': 'Point',
                    'source': drawLayer.getSource()
                });
				interaction.set('_mine', true);
				interaction.on('drawend', function(event) {
					event.feature.set('type', "beacon");
					if ($form) {
						event.feature.set('uuid', $form.lastUUID);
						event.feature.set('major', $form.lastMajor);
						event.feature.set('minor', $form.lastMinor = $form.lastMinor - 0 + 1);
						event.feature.set('power', $form.lastPower);
					}
				});
				map.addInteraction(interaction);
			}
		},
		'move' : {
			'label' : 'Move',
			'add' : function (map) {
				var interaction =  new ol.interaction.Modify({
                    'features': new ol.Collection(drawLayer.getSource().getFeatures())
                });
				interaction.set('_mine', true);
				map.addInteraction(interaction);
			}
//		},
//		'area' : {
//			'label' : 'Area',
//			'add' : function (map) {
//				var interaction =   new ol.interaction.Draw({
//                    'type': 'Polygon',
//                    'source': drawLayer.getSource()
//                });
//				interaction.set('_mine', true);
//				interaction.on('drawend', function(event) {
//					event.feature.set('type', "area");
//				});
//				map.addInteraction(interaction);
//			}
		}
	};

	var drawingBar = $('<div>');
	var drawingTools = $('<span>');
	drawingBar.append('Beacon Tools: ');
	for ( var mode in drawingInteractions) {
		var feature = drawingInteractions[mode];

		function createInput(label, id, options) {
			return $("<span>").append($("<label>", {
				'for' : id
			}).text(label)).append($("<input>", $.extend(options, {
				id : id
			})));
		}
		drawingTools.append(createInput(feature.label, feature.label, {
			'type' : "radio",
			'name' : "drawingtools",
			'click' : (function(mode) {
				return function() {
					activate(mode);
				};
			})(mode)
		}));
	}
	drawingTools.buttonset();
	drawingBar.append(drawingTools);

	drawingBar.append(' | ');

	drawingBar.append($('<form/>', {
		'id' : 'exportForm',
		'action' : baseURL + 'export',
		'method' : 'POST',
		'css' : {
			'display' : 'inline'
		}
	}).append($('<input/>', {
		'type' : 'hidden',
		'name' : ':content-type',
		'value' : 'application/json; charset=UTF-8'
	}), $('<input/>', {
		'id' : 'exportFile',
		'type' : 'hidden',
		'name' : 'beacons.json'
	}), $('<button>', {
		'text' : 'Export GeoJSON',
		'click' : function() {
			$('#exportFile').val(exportDrawFeatures(true));
			$('#exportForm').submit();
			return false;
		}
	}).button()));
	
	drawingBar.append(' | ');
	
	var uploadButton = $('<div>', {
		'class' : 'fileUpload btn btn-primary'
	}).appendTo(drawingBar);
	uploadButton.append(
		$('<span>', {
			'text' : 'Import GeoJSON'
		}),
		$('<input>', {
			'type' : 'file',
			'class' : 'upload',
			'id' : 'importFile',
			'change' : importDrawFeatures
		}
	));
	uploadButton.button();
	uploadButton.find('#importFile').change(importDrawFeatures);
//	drawingBar.append("Import GeoJSON", $("<input/>", {
//		"type": "file",
//		"id":"importFile",
//		"change":importDrawFeatures
//	}));
	
	drawingBar.append(' | ');

	drawingBar.append($('<form/>', {
		'id' : 'exportFormCSV',
		'action' : baseURL + 'export',
		'method' : 'POST',
		'css' : {
			'display' : 'inline'
		}
	}).append($('<input/>', {
		'type' : 'hidden',
		'name' : ':content-type',
		'value' : 'text/comma-separated-values; charset=UTF-8'
	}), $('<input/>', {
		'id' : 'exportFileCSV',
		'type' : 'hidden',
		'name' : 'beacons.csv'
	}), $('<button>', {
		'text' : 'Export CSV',
		'click' : function() {
			$('#exportFileCSV').val(exportDrawFeaturesCSV());
			$('#exportFormCSV').submit();
			return false;
		}
	}).button()));

	drawingBar.append(' | ', $('<button>', {
		'text' : 'Clear',
		'click' : function() {
			if (confirm("Are you sure to delete all?")) {
				drawLayer.getSource().clear();
			}
		}
	}).button());
	drawingBar.append(' | ', $('<button>', {
		'text' : 'Save',
		'click' : function() {
			saveBeacons();
		}
	}).button(), $('<span>', {
		'id' : 'saveBeacons_message',
		'css' : {
			'color': 'red'
		}
	}));
	activate('navigation');

	function showBeaconsInFloorplan(floorplan) {
		switch (floorplan.type) {
		case 'systemmap':
		case 'integrated':
			drawingBar.hide();
			break;
		default:
			drawingBar.show();
			break;
		}
		drawLayer.getSource().clear();
		editor.targetFloorplan = floorplan;
		if (!floorplan || !floorplan.beacons) {
			return;
		}
		floorplan.beacons.features = floorplan.beacons.features.filter(function(a) {
			return a.geometry.type != "Point" || a.properties.type; 
		});
		var features = format_geojson.readFeatures(floorplan.beacons, {
			'dataProjection' : getNoTransform(floorplan) ? mapProjection : editorProjection,
			'featureProjection' : mapProjection
		});
		drawLayer.getSource().addFeatures(features);
	}

	// Functions
	function activate(mode) {
		map.getInteractions().getArray().filter(function(interaction) {
			return interaction.get('_mine');
		}).forEach(function(interaction) {
			map.removeInteraction(interaction);
		});
		for (var m in drawingInteractions) {
			m == mode && drawingInteractions[m].add(map);
		}
		if (clickControl) {
			if (mode == 'navigation') {
				clickControl.activate();
			} else {
				clickControl.deactivate();
			}
		}
	}

	function clearSelection() {
		map.getInteractions().forEach(function(interaction) {
			interaction.getFeatures && interaction.get('_mine') && interaction.getFeatures().clear();
		});
	}

	var $form, $aform;
	
	function showForm(features) {
		if (!features || features.length == 0) {
			return;
		}
		var type = null;
		features.forEach(function(f){
			if (type == null) {
				type = f.get('type');
			} else {
				if (type != f.get('type')) {
					type = "invalid";
				}
			}
		})
		
		if (type == "beacon") {
			showBeaconForm(features);
		}
		if (type == "accessibility") {
			if (features.length == 1) {
				showAccForm(features[0]);
			} else {
				alert("Please select one accessibility feature");
			}			
		}
	}

	function showAccForm(feature) {
		if (!feature) {
			return;
		}

		if (!$aform) {
			$aform = $("<div>");
		} else {
			$aform.empty();
		}
		var params = feature.get('params');
			function createInput(label, id, size, options) {
				return $("<span>").append($("<label>", {
					'for' : id
				}).text(label)).append("<br>").append($("<input>", $.extend(options, {
					id : id
				})).attr("size", size)).append("<br>");
			}
			var $p = $("<p>").appendTo($aform);
			for(var k in params) {
				$p.append(createInput(k, k, 40, {
					type : "text"}));
			}
		
		for(var k in params) {
			$aform.find("#"+k).val(params.get(k));
		}
		
			
		$aform.selected = [feature];

		$aform.dialog({
			'width' : 'auto',
			'height' : 'auto',
			'resizable' : false,
			'modal' : true,
			'close' : function() {
				$aform.dialog("close");
				clearSelection();
				drawLayer.changed();
			}
		});
	}

	function showBeaconForm(features) {
		if (!features || features.length == 0) {
			return;
		}
		if (!$form) {
			$form = $("<div>");
			function createInput(label, id, size, options) {
				return $("<span>").append($("<label>", {
					'for' : id
				}).text(label)).append("<br>").append($("<input>", $.extend(options, {
					id : id
				})).attr("size", size)).append("<br>");
			}
			var $p = $("<p>").appendTo($form);
			$p.append(createInput("UUID:", "uuid", 40, {
				type : "text"
			})).append(createInput("Major:", "major", 5, {
				type : "number"
			})).append(createInput("Minor:", "minor", 5, {
				type : "number"
			})).append(createInput("Power:", "power", 5, {
				type : "number"
			}));
			var $p2 = $("<p>").append($("<button>", {
				click : function(e) {
					$form.selected.forEach(function(f) {
						if ($("#uuid").val() != $form.uuid) {
							f.set('uuid', $form.lastUUID = $("#uuid").val());
						}
						if ($("#major").val() != $form.major) {
							f.set('major', $form.lastMajor = $("#major").val());
						}
						if ($("#minor").val() != $form.minor) {
							f.set('minor', $form.lastMinor = $("#minor").val());
						}
						if ($("#power").val() != $form.power) {
							f.set('power', $form.lastPower = $("#power").val());
						}
					});
					$form.dialog("close");
					clearSelection();
					drawLayer.changed();
				}
			}).text("Update")).append(
					$(
							"<button>",
							{
								click : function(e) {
									if (confirm("Are you sure to delete " + $form.selected.length
											+ ($form.selected.length > 1 ? " beacons" : " beacon") + "?")) {
										$form.selected.forEach(function(feature) {
											drawLayer.getSource().removeFeature(feature);
										});
										$form.dialog("close");
									}
								},
								css : {
									float : "right"
								}
							}).text("Delete")).appendTo($form);
		}

		function checkCommonValue(features, name, obj) {
			obj[name] = features[0].get(name);
			features.forEach(function(f) {
				if (f.get(name) != obj[name]) {
					obj[name] = "";
				}
			});
			$form.find("#" + name).val(obj[name]);
		}
		[ "uuid", "major", "minor", "power" ].forEach(function(key) {
			checkCommonValue(features, key, $form);
		});

		$form.selected = features;

		$form.dialog({
			'width' : 'auto',
			'height' : 'auto',
			'resizable' : false,
			'modal' : true,
			'close' : function() {
				$form.dialog("close");
				clearSelection();
				drawLayer.changed();
			}
		});
	}

	function saveBeacons() {
		console.log(editor.targetFloorplan);
		var beacons = JSON.parse(format_geojson.writeFeatures(drawLayer.getSource().getFeatures(), {
			'dataProjection' : mapProjection,
			'featureProjection' : mapProjection
		}));
		setNoTransform(beacons);
		
		var query = {
			'_id' : editor.targetFloorplan._id
		}, update = {
			'$set' : {
				'beacons' : beacons,
			}
		};
		$('#saveBeacons_message').empty();
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
				$('#saveBeacons_message').text(error || text);
			}
		});

	}

	function importDrawFeatures() {
		fr = new FileReader();
		fr.onload = function(e) {
			var features = JSON.parse(fr.result);
			var fs = format_geojson.readFeatures(features, {
				'dataProjection' : getNoTransform(features) ? mapProjection : editorProjection,
				'featureProjection' : mapProjection
			});
			console.log(fs)
			drawLayer.getSource().addFeatures(fs);
		};
		fr.readAsText($("#importFile")[0].files[0]);		
	}
	
	function exportDrawFeatures(pretty) {
		activate(null);
		var ret = JSON.parse(format_geojson.writeFeatures(drawLayer.getSource().getFeatures(), {
			'dataProjection' : mapProjection,
			'featureProjection' : mapProjection
		}));
		setNoTransform(ret);
		if (options.beforeExport) {
			options.beforeExport(ret);
		}
		return JSON.stringify(ret);
	}

	function exportDrawFeaturesCSV() {
		activate(null);
		var ret = JSON.parse(format_geojson.writeFeatures(drawLayer.getSource().getFeatures(), {
			'dataProjection' : mapProjection,
			'featureProjection' : mapProjection
		}));
		var csv = 'uuid,major,minor,x,y,z,floor,power\n';
		ret.features.forEach(function(f) {
			var items = [f.properties.uuid, f.properties.major, f.properties.minor, 
			             f.geometry.coordinates[0], f.geometry.coordinates[1], 0, editor.targetFloorplan.floor, f.properties.power];
			csv += items.join(",") + "\n";			
		});
		return csv;
	}

	function getNoTransform(obj) {
		try {
			if (typeof(obj) == 'string') {
				obj = JSON.parse(obj);
			}
			var beacons = obj.beacons || obj;
			if (beacons.type == 'FeatureCollection') {
				return beacons._crs == 'epsg:3857';
			}
			console.error(beacons);
		} catch(e) {
			console.error(e);
		}
		return false;
	}

	function setNoTransform(obj) {
		if (obj.type == 'FeatureCollection') {
			obj._crs = 'epsg:3857';
			return;
		}
		console.error(obj);
	}

	return {
		'_editor' : editor,
		'drawingBar' : drawingBar,
		'getMapInfo' : function() {
			return mapInfo;
		},
		'drawLayer' : drawLayer,
		'showBeaconsInFloorplan' : showBeaconsInFloorplan
	};
}

