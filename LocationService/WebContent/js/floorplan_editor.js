/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation
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

function FloorplanEditor(map, options) {
	var editor = this;
	var baseURL = options.baseURL;
	var clickControl = options.clickControl;
	var mapProjection = options.mapProjection;
	var editorProjection = options.editorProjection;
	var drawingStyle = {};
	var floorplanDataType = "floorplans";

	for ( var intent in OpenLayers.Feature.Vector.style) {
		drawingStyle[intent] = new OpenLayers.Style($.extend(true, {}, OpenLayers.Feature.Vector.style[intent], {
			'strokeWidth' : 2,
			'fontSize' : '12px',
			'fontOpacity' : 0.4,
			'label' : '${label}',
		}), {
			'context' : {
				'label' : function(feature) {
					if (feature.attributes.type == "beacon") {
						var major = (feature.attributes.major == undefined) ? "" : feature.attributes.major;
						var minor = (feature.attributes.minor == undefined) ? "" : feature.attributes.minor;
						return major + "-" + minor;
					}
					return feature.attributes.label || '';
				}
			}
		});
		console.log([ intent, drawingStyle[intent] ]);

	}

	/*
	 * var drawLayer = new OpenLayers.Layer.Vector(options.title, { 'styleMap' :
	 * new OpenLayers.StyleMap(drawingStyle) });
	 */

	var smap = new OpenLayers.StyleMap(drawingStyle);
	// if use addUniqueValueRules there is a small problem with drawing
	// http://gis.stackexchange.com/questions/59312/openlayers-adduniquevaluerules-default-style-not-displaying
	// need to drawLayer.redraw() if attribute is changed after drawing
	/*
	 * smap.addUniqueValueRules("default", "type", { 'beacon' : { 'strokeColor' :
	 * "blue", 'fillColor' : "blue" }, 'default' : { 'strokeColor' : "black" }
	 * }); smap.styles['default'].rules.push(new OpenLayers.Rule({ elseFilter :
	 * true, symbolizer : smap.styles['default'].defaultStyle }));
	 */
	var drawLayer = new OpenLayers.Layer.Vector(options.title, {
		'styleMap' : smap
	});

	map.addLayers([ drawLayer ]);

	var drawingFeatures = {
		'select' : {
			'label' : 'Select',
			'control' : new OpenLayers.Control.SelectFeature(drawLayer, {
				'clickout' : true,
				'box' : true
			})
		},
		'beacon' : {
			'label' : 'Beacon',
			'control' : new OpenLayers.Control.DrawFeature(drawLayer, OpenLayers.Handler.Point)
		},
		'move' : {
			'label' : 'Move',
			'control' : new OpenLayers.Control.DragFeature(drawLayer, {
				'clickout' : true
			})
		}
	};
	var minor = 1;

	drawingFeatures.beacon.control.events.register('featureadded', this, function(e) {
		e.feature.attributes.type = "beacon";
		if ($form) {
			e.feature.attributes.uuid = $form.lastUUID;
			e.feature.attributes.major = $form.lastMajor;
			$form.lastMinor = e.feature.attributes.minor = $form.lastMinor - 0 + 1;
			e.feature.attributes.power = $form.lastPower;
		}
		drawLayer.redraw();
	});

	var boxSelect = false;
	var selected = [];
	drawingFeatures.select.control.events.register('featurehighlighted', this, function(e) {
		if (!boxSelect) {
			selected = [ e.feature ];
			showBeaconForm(selected);
		} else {
			selected.push(e.feature);
		}
	});
	drawingFeatures.select.control.events.register('boxselectionstart', this, function(e) {
		boxSelect = true;
		selected = [];
	});
	drawingFeatures.select.control.events.register('boxselectionend', this, function(e) {
		boxSelect = false;
		showBeaconForm(selected);
	});

	var drawingBar = $('<div>');
	var drawingTools = $('<span>');
	drawingBar.append('Beacon Tools: ');
	for ( var mode in drawingFeatures) {
		var feature = drawingFeatures[mode];
		map.addControl(feature.control);

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
		'text' : 'Save',
		'click' : function() {
			saveBeacons();
		}
	}).button());
	activate('navigation');

	function showBeaconsInFloorplan(floorplan) {
		editor.targetFloorplan = floorplan;
		if (!floorplan || !floorplan.beacons) {
			return;
		}
		drawLayer.removeAllFeatures();
		var geojson_format = new OpenLayers.Format.GeoJSON({
			'internalProjection' : mapProjection,
			'externalProjection' : editorProjection
		});
		var json = JSON.stringify(floorplan.beacons);
		drawLayer.addFeatures(geojson_format.read(json));
	}

	// Functions
	function activate(mode) {
		for ( var m in drawingFeatures) {
			var control = drawingFeatures[m].control;
			if (m == mode) {
				console.log(mode);
				control.activate();
			} else {
				control.deactivate();
			}
		}
		if (clickControl) {
			if (mode == 'navigation') {
				clickControl.activate();
			} else {
				clickControl.deactivate();
			}
		}
	}

	var $form;

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
							$form.lastUUID = f.attributes.uuid = $("#uuid").val();
						}
						if ($("#major").val() != $form.major) {
							$form.lastMajor = f.attributes.major = $("#major").val();
						}
						if ($("#minor").val() != $form.minor) {
							$form.lastMinor = f.attributes.minor = $("#minor").val();
						}
						if ($("#power").val() != $form.power) {
							$form.lastPower = f.attributes.power = $("#power").val();
						}
					});
					$form.dialog("close");
					drawingFeatures.select.control.unselectAll();
					drawLayer.redraw();
				}
			}).text("Update")).append(
					$(
							"<button>",
							{
								click : function(e) {
									if (confirm("Are you sure to delete " + $form.selected.length
											+ ($form.selected.length > 1 ? " beacons" : " beacon") + "?")) {
										drawLayer.removeFeatures($form.selected);
										$form.dialog("close");
									}
								},
								css : {
									float : "right"
								}
							}).text("Delete")).appendTo($form);
		}

		function checkCommonValue(features, name, obj) {
			obj[name] = features[0].attributes[name]
			features.forEach(function(f) {
				if (f.attributes[name] != obj[name]) {
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
				drawingFeatures.select.control.unselectAll();
				drawLayer.redraw();
			}
		});
	}

	function saveBeacons() {
		console.log(editor.targetFloorplan);
		var geojson_format = new OpenLayers.Format.GeoJSON({
			'internalProjection' : mapProjection,
			'externalProjection' : editorProjection
		});
		var beacons = JSON.parse(geojson_format.write(drawLayer.features));

		var query = {
			'_id' : editor.targetFloorplan._id
		}, update = {
			'$set' : {
				'beacons' : beacons,
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

	}

	function exportDrawFeatures(pretty) {
		activate(null);
		var geojson_format = new OpenLayers.Format.GeoJSON({
			'internalProjection' : mapProjection,
			'externalProjection' : editorProjection
		});
		var ret = JSON.parse(geojson_format.write(drawLayer.features, pretty));
		if (options.beforeExport) {
			options.beforeExport(ret);
		}
		return JSON.stringify(ret);
	}

	function exportDrawFeaturesCSV() {
		activate(null);
		var geojson_format = new OpenLayers.Format.GeoJSON({
			'internalProjection' : mapProjection,
			'externalProjection' : editorProjection
		});
		var ret = JSON.parse(geojson_format.write(drawLayer.features));
		var csv = "";
		ret.features.forEach(function(f) {
			var items = [f.properties.uuid, f.properties.major, f.properties.minor, 
			             f.geometry.coordinates[0], f.geometry.coordinates[1], 0, editor.targetFloorplan.floor];
			csv += items.join(",") + "\n";			
		});
		return csv;
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

