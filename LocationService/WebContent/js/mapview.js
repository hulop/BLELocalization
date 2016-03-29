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

function calcDistance(x1, y1, x2, y2, projection) {
	var line = new OpenLayers.Geometry.LineString([ new OpenLayers.Geometry.Point(x1, y1), new OpenLayers.Geometry.Point(x2, y2) ]);
	return projection ? line.getGeodesicLength(projection) : line.getLength();
}

function MapViewer(mapdiv, options) {
	options = options || {};
	var baseURL = options.baseURL || '/LocationService/';
	var map = null, baseLayer = null, mapInfo = null, mapEditor = null;
	var defaultStyle = OpenLayers.Feature.Vector.style['default'];

	// Projections
	var mapProjection = options.mapProjection || new OpenLayers.Projection('EPSG:3857');
	var externalProjection = options.externalProjection || mapProjection;
	var mouseProjection = options.mouseProjection || externalProjection;
	var editorProjection = options.editorProjection || externalProjection;

	// Layers
	var markerLayer = new OpenLayers.Layer.Vector('Marker', {
		'styleMap' : getStyleMap({
			'graphicHeight' : 25,
			'graphicWidth' : 21,
			'graphicXOffset' : -10.5,
			'graphicYOffset' : -25,
			'graphicOpacity' : 1.0,
			'label' : '${label}',
			'labelYOffset' : 30
		}, [ {
			'intent' : 'default',
			'property' : 'type',
			'symbolizers' : {
				'default' : {
					'externalGraphic' : OpenLayers.Util.getImageLocation('marker.png')
				},
				'blue' : {
					'externalGraphic' : OpenLayers.Util.getImageLocation('marker-blue.png'),
				},
				'green' : {
					'externalGraphic' : OpenLayers.Util.getImageLocation('marker-green.png'),
				},
				'gold' : {
					'externalGraphic' : OpenLayers.Util.getImageLocation('marker-gold.png'),
				}
			}
		} ]),
		'eventListeners' : {
			'featureclick' : function(e) {
				if (options.featureclick) {
					options.featureclick(e);
				}
				return false;
			}
		}
	});
	var areaLayer = new OpenLayers.Layer.Vector('Area', {
		'styleMap' : getStyleMap({
			'strokeColor' : '#2980b9',
			'fillColor' : '#2980b9',
			'fillOpacity' : 0.2,
			'fontSize' : '12px',
			'fontColor' : 'red',
			'label' : '${label}'
		})
	});
	var networkLayer = new OpenLayers.Layer.Vector('Network', {
	// TODO
	});
	var gridLayer = new OpenLayers.Layer.Vector('Grid', {
		'styleMap' : getStyleMap({}, [ {
			'intent' : 'default',
			'property' : 'type',
			'symbolizers' : {
				'origin' : {
					'strokeColor' : 'black',
					'strokeWidth' : 2
				},
				'normal' : {
					'strokeColor' : 'lime',
					'strokeWidth' : 0.25
				}
			}
		} ])
	});
	var beaconLayer = new OpenLayers.Layer.Vector('Beacon', {
		'styleMap' : getStyleMap({
			'strokeColor' : 'red',
			'strokeWidth' : 1,
			'fillColor' : 'white',
			'pointRadius' : 8,
			'label' : '${label}'
		})
	});
	var particleLayer = new OpenLayers.Layer.Vector('Particle', {
		'styleMap' : getStyleMap({
			'strokeWidth' : 1,
			'pointRadius' : 1,
			'strokeColor' : 'black',
			'fillColor' : 'black'
		})
	});

	// Controls
	var mouseControl = new OpenLayers.Control.MousePosition({
		'formatOutput' : function(lonLat) {
			lonLat.transform(mapProjection, mouseProjection);
			if (options.mousePositionFormatOutput) {
				return options.mousePositionFormatOutput(lonLat, mapInfo);
			} else {
				return lonLat.lon + ', ' + lonLat.lat;
			}
		}
	});
	var clickControl = null;

	// Functions
	function getStyleMap(style, options) {
		var styleMap = new OpenLayers.StyleMap($.extend(true, {}, defaultStyle, style || {}));
		if (options) {
			options.forEach(function(option) {
				styleMap.addUniqueValueRules(option.intent, option.property, option.symbolizers);
			});
		}
		return styleMap;
	}

	function showMap(info) {
		mapInfo = info;
		console.log(mapInfo);

		var divWidth = $('#' + mapdiv).width(), divHeight = $('#' + mapdiv).height();
		var scale = Math.min(divWidth / mapInfo.width, divHeight / mapInfo.height);
		var mapLayer = new OpenLayers.Layer.Image('Image', mapInfo.imageURL, new OpenLayers.Bounds(0, 0, mapInfo.width, mapInfo.height), new OpenLayers.Size(
				mapInfo.width * scale, mapInfo.height * scale), {
			'numZoomLevels' : 5
		});
		if (map && baseLayer) {
			map.removeLayer(baseLayer);
			map.addLayer(mapLayer);
		} else {
			window.$openlayer_map_instance = // Need map instance for ZOOM control
			map = new OpenLayers.Map({
				'projection' : mapProjection,
				'div' : mapdiv,
				'layers' : [ mapLayer, gridLayer, areaLayer, networkLayer, beaconLayer, particleLayer, markerLayer ],
				'controls' : [ mouseControl, new OpenLayers.Control.Navigation(), new OpenLayers.Control.PanZoomBar(), new OpenLayers.Control.LayerSwitcher() ]
			});
			if (options.mapLayer) {
				map.addLayer(options.mapLayer);
			}
			if (options.click) {
				clickControl = new (OpenLayers.Class(OpenLayers.Control, {
					initialize : function() {
						OpenLayers.Control.prototype.initialize.apply(this, arguments);
						this.handler = new OpenLayers.Handler.Click(this, {
							'click' : function(e) {
								var lonLat = map.getLonLatFromPixel(e.xy);
								lonLat.transform(mapProjection, mouseProjection);
								options.click(lonLat.lon, lonLat.lat);
							}
						}, {
							'single' : true,
							'double' : false
						});
					}
				}))();
				map.addControl(clickControl);
				clickControl.activate();
			}
			if (options.editor) {
				mapEditor = new options.editor(map, {
					'title' : 'Drawing',
					'onImport' : options.onImport,
					'beforeExport' : options.beforeExport,
					'baseURL' : baseURL,
					'clickControl': clickControl,
					'mapProjection' : mapProjection,
					'editorProjection': editorProjection
					
				});
				$('#' + mapdiv).before(mapEditor.drawingBar);
			}
		}
		var noZoom = options.mapLayer && baseLayer;
		baseLayer = mapLayer;
		if (!noZoom) {
			map.zoomToMaxExtent();
		}
	}

	function drawMarker(markers, projection) {
		markerLayer.removeAllFeatures();
		var features = [];
		markers.forEach(function(marker) {
			features.push(new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Point(marker.x, marker.y).transform(projection || externalProjection,
					mapProjection), {
				'type' : marker.type || 'default',
				'label' : marker.label || '',
				'options' : marker.options
			}));
		});
		markerLayer.addFeatures(features);
	}

	function drawBeacon(features, projection) {
		drawFeatures(features, beaconLayer, projection);
	}

	function drawArea(features, projection) {
		drawFeatures(features, areaLayer, projection);
		// if (options.mapLayer) {
		// map.zoomToExtent(areaLayer.getDataExtent());
		// }
	}

	function drawNetwork(features, projection) {
		drawFeatures(features, networkLayer, projection);
	}

	function drawFeatures(features, layer, projection) {
		layer.removeAllFeatures();
		var geojson_format = new OpenLayers.Format.GeoJSON({
			'internalProjection' : mapProjection,
			'externalProjection' : projection || externalProjection
		});
		layer.addFeatures(geojson_format.read(JSON.stringify({
			'type' : 'FeatureCollection',
			'features' : features
		})));
	}

	function drawParticle(particles, projection) {
		particleLayer.removeAllFeatures();
		var features = [];
		function addPoint(x1, y1) {
			var f = {
	            "type": "Feature",
	            "properties": {
	            },
	            "geometry": {
	                "type": "Point",
	                "coordinates": [
	                    x1,
	                    y1
	                ]
	            }
	        }
			features.push(f);
		}
		for(var i = 0; i < particles.length; i++) {
			var p = particles[i];
			addPoint(p.x, p.y);
		}
		drawFeatures(features, particleLayer, projection);
		particleLayer.addFeatures(features);
	}

	function drawGrid(start, step) {
		gridLayer.removeAllFeatures();
		var features = [];
		function addLine(x1, y1, x2, y2, origin) {
			var p1 = new OpenLayers.Geometry.Point(x1, y1);
			var p2 = new OpenLayers.Geometry.Point(x2, y2);
			features.push(new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([ p1, p2 ]), {
				'type' : origin ? 'origin' : 'normal'
			}));
		}
		for ( var s = start.x % step.x, i = 0, x; Math.floor(x = s + i * step.x) <= mapInfo.width; i++) {
			addLine(x, 0, x, mapInfo.height, Math.floor(x - start.x) == 0);
		}
		for ( var s = start.y % step.y, i = 0, y; Math.floor(y = s + i * step.y) <= mapInfo.height; i++) {
			addLine(0, y, mapInfo.width, y, Math.floor(y - start.y) == 0);
		}
		gridLayer.addFeatures(features);
	}

	return {
		'show' : showMap,
		'getEditor' : function(){return mapEditor},
		'drawArea' : drawArea,
		'drawGrid' : drawGrid,
		'drawBeacon' : drawBeacon,
		'drawNetwork' : drawNetwork,
		'drawMarker' : drawMarker,
		'drawParticle' : drawParticle,
		'getMapInfo' : function() {
			return mapInfo;
		}
	};
}
