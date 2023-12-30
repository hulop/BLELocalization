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

function MapViewer(mapdiv, options) {
	options = options || {};
	var baseURL = options.baseURL || '../';
	var map = null, baseLayer = null, mapInfo = null, mapEditor = null;

	// Projections
	var mapProjection = options.mapProjection || 'EPSG:3857';
	var externalProjection = options.externalProjection || mapProjection;
	var mouseProjection = options.mouseProjection || externalProjection;
	var editorProjection = options.editorProjection || externalProjection;

	// Layers
	var markerLayer = new ol.layer.Vector({
		'source' : new ol.source.Vector(),
		'zIndex' : 2,
		'style' : function(feature) {
			if (isTraliler(feature)) {
				return;
			}
			var file = 'marker.png';
			switch (feature.get('type')) {
			case 'blue' :
			case 'green' :
			case 'gold':
				file = 'marker-' + feature.get('type') + '.png';
				break;
			}
			return new ol.style.Style({
				'image' : new ol.style.Icon({
					'anchor' : [ 0.5, 1 ],
					'anchorXUnits' : 'fraction',
					'anchorYUnits' : 'fraction',
					'src' : 'images/' + file
				})
            });
		}
	});
	var gridStyles = {
			'normal' : {
				'color' : 'lime',
				'width' : 0.25
			},
			'scale' : {
				'color' : 'red',
				'width' : 0.25
			},
			'origin' : {
				'color' : 'black',
				'width' : 1
			}
	};
	var gridLayer = new ol.layer.Vector({
		'source' : new ol.source.Vector(),
		'zIndex' : 1,
		'style' : function(feature) {
			var type = feature.getProperties().type;
			return type && new ol.style.Style({
				'stroke' : new ol.style.Stroke(gridStyles[type])
			})
		}
	});

	// Controls
	var mousePositionControl = new ol.control.MousePosition({
	    'coordinateFormat': function(coord) {
	    	return ol.coordinate.format(coord, '{x}m, {y}m', 2);
	    },
	    'projection': new ol.proj.Projection({
			'code' : mouseProjection
		}),   
	    'undefinedHTML': '&nbsp;'
	    });

	// Functions
	function showMap(info) {
		mapInfo = info;
		console.log(mapInfo);
		
		var divWidth = $('#' + mapdiv).width(), divHeight = $('#' + mapdiv).height();
		var scale = Math.min(divWidth / mapInfo.width, divHeight / mapInfo.height);
		var imageExtent = [0, 0, mapInfo.width, mapInfo.height];
		var mapLayer = new ol.layer.Image({
			'source' : new ol.source.ImageStatic({
				'url' : mapInfo.imageURL,
				'imageExtent' : imageExtent,
				'projection' : 'EPSG:3857'
			})
		});
		if (map && baseLayer) {
			map.removeLayer(baseLayer);
			map.addLayer(mapLayer);
			map.setView(new ol.View({
				'extent' : imageExtent
			}));
		} else {
			var layers = [ mapLayer, gridLayer, markerLayer ];
			options.mapLayer && layers.push(options.mapLayer);
			var controls = ol.control.defaults({});
			controls.push(mousePositionControl);
			// Need map instance for ZOOM control
			window.$openlayer_map_instance = 
			map = new ol.Map({
				'projection' : mapProjection,
				'target' : mapdiv,
				'layers' : layers,
				'controls' : controls,
				'view' : new ol.View({
					'extent' : imageExtent
				})
			});
			var mapClickControl = null;
			if (options.click) {
				function clickHandler(event) {
					var latLng = ol.proj.transform(event.coordinate, mapProjection, externalProjection);
					options.click(latLng[0], latLng[1]);
				}
				mapClickControl = {
						'activate' : function() {
							map.on('click', clickHandler);
						},
						'deactivate' : function() {
							map.un('click', clickHandler);
						}
				};
				mapClickControl.activate();
			}
			if (options.editor) {
				mapEditor = new options.editor(map, {
					'title' : 'Drawing',
					'onImport' : options.onImport,
					'beforeExport' : options.beforeExport,
					'baseURL' : baseURL,
					'clickControl': mapClickControl,
					'mapProjection' : mapProjection,
					'editorProjection': editorProjection
					
				});
				$('#' + mapdiv).before(mapEditor.drawingBar);
			}
			map.on('click', function(event) {
				event.originalEvent.ctrlKey && onSampleClick(event.pixel);
			});
		}
		var noZoom = options.mapLayer && baseLayer;
		baseLayer = mapLayer;
		if (!noZoom) {
			map.getView().fit(imageExtent, map.getSize()); 		
		}
	}

	function drawMarker(markers, projection) {
		var source = markerLayer.getSource(); 
		source.clear();
		var features = markers.map(function(marker) {
			var point = ol.proj.transform([marker.x, marker.y], projection || externalProjection, mapProjection);
			return new ol.Feature({
				'geometry': new ol.geom.Point(point),
				'type' : marker.type || 'default',
				'label' : marker.label || '',
				'options' : marker.options
			});
		});
		source.addFeatures(features);
	}

	function drawGrid(origin, ppm) {
		var source = gridLayer.getSource();
		source.clear();
		var features = [];
		function addLine(x1, y1, x2, y2, index) {
			var p1 = ol.proj.transform([x1, y1], externalProjection, mapProjection);
			var p2 = ol.proj.transform([x2, y2], externalProjection, mapProjection);
			var feature = new ol.Feature({
				'geometry': new ol.geom.LineString([p1, p2]),
				'type' : index == 0 ? 'origin' : index % 10 == 0 ? 'scale' : 'normal'
			});
			features.push(feature);
		}
		var width = mapInfo.width / ppm.x * 2, x_min = -(width / 4) - (origin.x / ppm.x), x_max = width + x_min;
		var height = mapInfo.height / ppm.y * 2, y_min = -(height / 4) - (origin.y / ppm.y), y_max = height + y_min;
		x_min = Math.floor(x_min / 10) * 10;
		y_min = Math.floor(y_min / 10) * 10;
		x_max = Math.ceil(x_max / 10) * 10;
		y_max = Math.ceil(y_max/ 10) * 10;
		for (var x = x_min; x <= x_max; x++) {
			addLine(x, y_min, x, y_max, x);
		}
		for (var y = y_min; y <= y_max; y++) {
			addLine(x_min, y, x_max, y, y);
		}
		source.addFeatures(features);
	}

	var selectedSample;
	function isTraliler(feature) {
		try {
			 return selectedSample && feature.get('options').sampling.data.timestamp >= selectedSample;
		} catch (e) {
		}
	}

	function onSampleClick(pixel) {
		selectedSample = map.forEachFeatureAtPixel(pixel, function(feature) {
			try {
				return feature.get('options').sampling.data.timestamp;
			} catch (e) {
			}
		});
		if (selectedSample) {
			markerLayer.changed();
			setTimeout(function() {
				var source = markerLayer.getSource(); 
				var trailers = source.getFeatures().filter(isTraliler);
				var del = trailers.length && window.confirm('Delete ' + trailers.length + ' sampling data?');
				selectedSample = null;
				markerLayer.changed();
				if (del) {
					// console.log(trailers);
					var query = JSON.stringify({
						"_id": {
							"$in": trailers.map(function(feature) {
								return feature.get('options').sampling._id;
							})
						}
					});
					$.ajax('data?type=samplings&query=' + encodeURIComponent(query), {
						'method' : 'DELETE',
						'dataType' : 'json',
						'success': function(data) {
							console.log(["DELETE", data])
							trailers.forEach(function(feature) {
								source.removeFeature(feature);
							});
						},
						'error': function(xhr, text, error) {
							$('#message').text(error || text);
						}
					})	
				}
			});
		}
	}

	return {
		'show' : showMap,
		'getEditor' : function(){return mapEditor},
		'drawGrid' : drawGrid,
		'drawMarker' : drawMarker,
		'getMapInfo' : function() {
			return mapInfo;
		}
	};
}