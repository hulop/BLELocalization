/*
 * Map Editor
 */
function EnvEditor(map, options) {
	var baseURL = options.baseURL;
	var clickControl = options.clickControl;
	var drawingStyle = {};
	for ( var intent in OpenLayers.Feature.Vector.style) {
		drawingStyle[intent] = new OpenLayers.Style($.extend(true, {},
				OpenLayers.Feature.Vector.style[intent], {
					'strokeWidth' : 2,
					'fontSize' : '12px',
					'fontOpacity' : 0.4,
					'label' : '${label}',
				}), {
			'context' : {
				'label' : function(feature) {
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
	smap.addUniqueValueRules("default", "type", {
		'beacon' : {
			'strokeColor' : "blue",
			'fillColor' : "blue"
		},
		'wall' : {
			'strokeColor' : "black"
		},
		'walk' : {
			'strokeColor' : "green"
		},
		'grid' : {
			'strokeColor' : "gray",
			'fillColor' : "lightgray"
		},
		'default' : {
			'strokeColor' : "black"
		}
	});
	smap.styles['default'].rules.push(new OpenLayers.Rule({
		elseFilter : true,
		symbolizer : smap.styles['default'].defaultStyle
	}));
	var drawLayer = new OpenLayers.Layer.Vector(options.title, {
		'styleMap' : smap
	});

	map.addLayers([ drawLayer ]);

	var drawingFeatures = options.drawingFeatures
			|| {
				'navigation' : {
					'label' : 'Navigation',
					'control' : new OpenLayers.Control.Navigation()
				},
				'beacon' : {
					'label' : 'Add Beacon',
					'control' : new OpenLayers.Control.DrawFeature(drawLayer,
							OpenLayers.Handler.Point)
				},
				'wall' : {
					'label' : 'Add Wall',
					'control' : new OpenLayers.Control.DrawFeature(drawLayer,
							OpenLayers.Handler.Path)
				},
				'walk' : {
					'label' : 'Add Walk',
					'control' : new OpenLayers.Control.DrawFeature(drawLayer,
							OpenLayers.Handler.Path)
				},
				'grid' : {
					'label' : 'Add Grid',
					'control' : new OpenLayers.Control.DrawFeature(drawLayer,
							OpenLayers.Handler.RegularPolygon)
				},
				'property' : {
					'label' : 'Property',
					'control' : new OpenLayers.Control.SelectFeature(drawLayer,
							{
								'clickout' : true
							})
				},
				'modify' : {
					'label' : 'Modify',
					'control' : new OpenLayers.Control.ModifyFeature(drawLayer,
							{
								'clickout' : true
							})
				},
				'drag' : {
					'label' : 'Drag',
					'control' : new OpenLayers.Control.DragFeature(drawLayer, {
						'clickout' : true
					})
				},
				'del' : {
					'label' : 'Delete',
					'control' : new OpenLayers.Control.SelectFeature(drawLayer,
							{
								'clickout' : true
							})
				}
			};
	var minor = 1;

	drawLayer.events.register('sketchcomplete', this, function(f) {
		console.log(f);
	});

	drawingFeatures.beacon.control.events
			.register(
					'featureadded',
					this,
					function(e) {
						e.feature.attributes.type = "beacon";
						e.feature.attributes.uuid = "00000000-0000-0000-0000-000000000000";
						e.feature.attributes.major = 1;
						e.feature.attributes.minor = minor++;
						e.feature.attributes.power = -12;
						e.feature.attributes.interval = 100;
						e.feature.attributes.memo = "";
						drawLayer.redraw();
					});
	drawingFeatures.wall.control.events.register('featureadded', this,
			function(e) {
				e.feature.attributes.type = "wall";
				e.feature.attributes.height = 3.0;
				e.feature.attributes.decay = -10;
				e.feature.attributes.memo = "";
				drawLayer.redraw();
			});
	drawingFeatures.walk.control.events.register('featureadded', this,
			function(e) {
				e.feature.attributes.type = "walk";
				e.feature.attributes.height = 1.0;
				e.feature.attributes.repeat = 1;
				e.feature.attributes.memo = "";
				drawLayer.redraw();
			});
	drawingFeatures.grid.control.events.register('featureadded', this,
			function(e) {
				e.feature.attributes.type = "grid";
				e.feature.attributes.height = 1.0;
				e.feature.attributes.interval = 1.0;
				e.feature.attributes.repeat = 10;
				e.feature.attributes.memo = "";
				drawLayer.redraw();
			});

	drawingFeatures.del.control.events
			.register(
					'featurehighlighted',
					this,
					function(e) {
						if (confirm('Delete '
								+ (e.feature.attributes.label || 'this feature')
								+ '?')) {
							drawLayer.removeFeatures([ e.feature ]);
						} else {
							e.object.unselect(e.feature);
						}
					});

	drawingFeatures.property.control.events.register('featurehighlighted',
			this, function(e) {
				drawingFeatures.property.selected_feature = e.feature;
				showProps(e.feature);
			});

	function showProps(feature) {
		drawingFeatures.property.select.empty();
		for ( var key in feature.attributes) {
			drawingFeatures.property.select.append($("<option>").text(key), {
				value : key
			});
		}
		var e = document.createEvent("HTMLEvents");
		e.initEvent("change", true, true);
		drawingFeatures.property.select[0].dispatchEvent(e)

	}

	var drawingBar = $('<div>');
	drawingBar.append('Drawing Tools: ');
	for ( var mode in drawingFeatures) {
		var feature = drawingFeatures[mode];
		map.addControl(feature.control);
		drawingBar.append(feature.button = $('<button>', {
			'text' : feature.label,
			'click' : (function(mode) {
				return function() {
					activate(mode);
				};
			})(mode)
		})).append(" ");
		if (mode == "property") {
			drawingBar.append(feature.select = $('<select>', {})).append(" ")
					.append(feature.input = $('<input>', {
						type : "text",
						size : 20
					})).append(" ");
			feature.select.change((function(that) {
				return function(e) {
					var key = this.options[this.selectedIndex].value;
					that.input.val(that.selected_feature.attributes[key]);
				}
			})(feature));
			feature.input
					.change((function(that) {
						return function(e) {
							var key = that.select[0].options[that.select[0].selectedIndex].value;
							if (this.value.match(/^-?[0-9]+(\.[0-9]+)?$/)) {
								that.selected_feature.attributes[key] = parseFloat(this.value);
							} else {
								that.selected_feature.attributes[key] = this.value;
							}
						}
					})(feature));
		}
	}

	drawingBar.append(' | ');

	drawingBar.append($('<button>', {
		'text' : 'Delete All',
		'click' : function() {
			if (confirm('Delete all features?')) {
				drawLayer.removeAllFeatures();
			}
		}
	}));

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
		'name' : 'features.json'
	}), $('<button>', {
		'text' : 'Export',
		'click' : function() {
			$('#exportFile').val(exportDrawFeatures(true));
			$('#exportForm').submit();
			return false;
		}
	})));

	drawingBar.append(' | ');

	drawingBar.append($('<form/>', {
		'id' : 'importForm',
		'method' : 'POST',
		'css' : {
			'display' : 'inline'
		}
	}).append($('<input/>', {
		'type' : 'file',
		'name' : 'file'
	}), $('<button>', {
		'text' : 'Import',
		'click' : function() {
			$.ajax(baseURL + 'import', {
				'method' : 'POST',
				'contentType' : false,
				'processData' : false,
				'data' : new FormData($('#importForm')[0]),
				'dataType' : 'json',
				'success' : function(data) {
					importDrawFeatures(data);
				},
				'error' : function() {
					console.log('error');
				}
			});
			return false;
		}
	})));
	activate('navigation');

	return {
		'drawingBar' : drawingBar,
		'layer' : drawLayer,
		'getMapInfo' : function() {
			return mapInfo;
		}
	};

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
			drawingFeatures[m].button.css({
				'background-color' : m == mode ? 'yellow' : ''
			});
		}
		if (clickControl) {
			if (mode == 'navigation') {
				clickControl.activate();
			} else {
				clickControl.deactivate();
			}
		}
	}

	function importDrawFeatures(features) {
		console.log(features);
		if (options.onImport) {
			options.onImport(features);
		}
		activate(null);
		drawLayer.removeAllFeatures();
		var geojson_format = new OpenLayers.Format.GeoJSON({
			'internalProjection' : mapProjection,
			'externalProjection' : editorProjection
		});
		var json = JSON.stringify(features);
		drawLayer.addFeatures(geojson_format.read(json));
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
}