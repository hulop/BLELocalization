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

function $hokoukukan(geojson) {
	var nodeMap = {}, entMap = {}, links = [], areas = [];

	geojson.features.forEach(function(f) {
		if (f.properties.link_id || f.properties['リンクID']) {
			links.push(f);
			return;
		}
		if (f.properties.hulop_area_id) {
			areas.push(f);
			return;
		}
		var node_id = f.properties.node_id || f.properties['ノードID'];
		if (node_id) {
			nodeMap[node_id] = f;
		} else if (f.properties.facil_id) {
			for ( var key in f.properties) {
				if (/ent\d+_node/.test(key)) {
					entMap[f.properties[key]] = true;
				}
			}
		} else if (f.properties['出入口ID']) {
			entMap[f.properties['対応ノードID']] = true;
		}
	});

	function node2xy(node, anchor) {
		return node.geometry.type == 'Point' && latlon2xy({
			'lon' : node.geometry.coordinates[0],
			'lat' : node.geometry.coordinates[1]
		}, anchor);
	}

	function getStartNode(f) {
		return f.properties.start_id || f.properties['起点ノードID'];
	}

	function getEndNode(f) {
		return f.properties.end_id || f.properties['終点ノードID'];
	}

	function countLink(f) {
		var count = 0;
		for ( var key in f.properties) {
			if (/(接続リンクID\d+|link\d+_id)/.test(key)) {
				count++;
			}
		}
		return count;
	}

	function getHeight(node) {
		var height = parseInt(node.properties['高さ'] || node.properties.floor);
		return height < 1 ? height : height - 1;
	}

	function calculateExtent(anchor) {
		var extent = [ Infinity, Infinity, -Infinity, -Infinity ];
		function expand(coordinates) {
			if (coordinates && coordinates.length > 0) {
				if (isNaN(coordinates[0])) {
					coordinates.forEach(expand);
				} else {
					var local = latlon2xy({
						'lon' : coordinates[0],
						'lat' : coordinates[1]
					}, anchor);
					extent[0] = Math.min(extent[0], local.x);
					extent[1] = Math.min(extent[1], local.y);
					extent[2] = Math.max(extent[2], local.x);
					extent[3] = Math.max(extent[3], local.y);
				}
			}
		}
		geojson.features.forEach(function(f) {
			expand(f.geometry.coordinates);
		});
		return extent;
	}

	function drawBack(ctx, layer, anchor) {
		// draw black bold line
		function traceCoordinates(coordinates) {
			ctx.globalCompositeOperation = 'source-over';
			ctx.lineWidth = 50;
			ctx.strokeStyle = 'black';
			ctx.lineJoin = ctx.lineCap = 'round';
			ctx.beginPath();
			tracePath(ctx, anchor, coordinates);
			ctx.stroke();
		}

		links.forEach(function(f) {
			var s = nodeMap[getStartNode(f)], t = nodeMap[getEndNode(f)];
			if (s && t && getHeight(s) == layer.floor_num && getHeight(t) == layer.floor_num) {
				traceCoordinates(f.geometry.coordinates);
			}
		});
		areas.forEach(function(f) {
			var properties = f.properties;
			if (properties.hulop_area_localization) {
				var height = parseInt(properties.hulop_area_height);
				height = height < 1 ? height : height - 1;
				if (height == layer.floor_num) {
					traceCoordinates(f.geometry.coordinates[0]);
				}
			}
		});
	}

	function getLineWidth(f) {
		switch (f.properties.width) {
		case 1:
			return 1;
		case 2:
			return 2;
		}
		switch (f.properties['有効幅員']) {
		case '0':
			return 1;
		case '1':
			return 1.5;
		case '2':
			return 2;
		}
		return parseFloat($('#default_width').val()) || 5;
	}

	function isSlope(f) {
		var s = nodeMap[getStartNode(f)], t = nodeMap[getEndNode(f)];
		return s && t && getHeight(s) != getHeight(t)
	}

	function drawLinks(ctx, layer, anchor) {

		function fillArea(flag, color) {
			areas.forEach(function(f) {
				var properties = f.properties;
				if (parseFloat(properties.hulop_area_localization) == flag) {
					var height = parseInt(properties.hulop_area_height);
					height = height < 1 ? height : height - 1;
					if (height == layer.floor_num) {
						ctx.fillStyle = color;
						ctx.beginPath();
						tracePath(ctx, anchor, f.geometry.coordinates[0]);
						ctx.closePath();
						ctx.fill();
					}
				}
			});
		}
		// fill localization area
		fillArea(2, 'white');

		links.forEach(function(f) {
			var s_id = getStartNode(f), t_id = getEndNode(f)
			var s = nodeMap[s_id], t = nodeMap[t_id];
			if (!s || !t) {
				return;
			}
			if ((entMap[s_id] && countLink(s) == 1 || entMap[t_id] && countLink(t) == 1)
					&& parseFloat(f.properties['リンク延長'] || f.properties.distance) < 3) {
				return;
			}
			if (s && t && getHeight(s) == layer.floor_num && getHeight(t) == layer.floor_num) {
				// ctx.globalCompositeOperation = 'destination-out';
				ctx.lineWidth = getLineWidth(f);
				ctx.strokeStyle = 'white';
				ctx.lineJoin = ctx.lineCap = 'round';
				ctx.beginPath();
				tracePath(ctx, anchor, f.geometry.coordinates);
				ctx.stroke();
			}
		});

		links.forEach(function(f) {
			var lineWidth = 3;
			var lineCap = 'round';
			var style;
			switch (f.properties.route_type) {
			case 5: // escalator
				style = 'lime'; // (0,255,0)
				lineWidth = 1.8;
				lineCap = 'butt';
				break;
			case 6: // stair
				style = 'blue'; // (0,0,255)
				break;
			case 2: // moving walkway
				style = isSlope(f) ? 'lime' : 'cyan'; // (0,255,255)
				lineWidth = 1.8;
				lineCap = 'butt';
				break;
			case 4:	// elevator
			case undefined:
				break;
			default:
				if (isSlope(f)) {
					style = 'blue';
					lineWidth = getLineWidth(f);
				}
				break;
			}
			switch (f.properties['経路の種類']) {
			case '11': // escalator
				style = 'lime'; // (0,255,0)
				lineWidth = 1.8;
				lineCap = 'butt';
				break;
			case '12': // stair
				style = 'blue'; // (0,0,255)
				break;
			case '7': // moving walkway
				style = isSlope(f) ? 'lime' : 'cyan'; // (0,255,255)
				lineWidth = 1.8;
				lineCap = 'butt';
				break;
			case '10': // elevator
			case undefined:
				break;
			default:
				if (isSlope(f)) {
					style = 'blue';
					lineWidth = getLineWidth(f);
				}
				break;
			}
			if (!style) {
				return;
			}
			var s = nodeMap[getStartNode(f)], t = nodeMap[getEndNode(f)];
			if (s && t) {
				var start_height = getHeight(s), end_height = getHeight(t), start_draw, end_draw;
				if (start_height != layer.floor_num || end_height != layer.floor_num) {
					if (start_height == layer.floor_num) {
						start_draw = 0;
						end_draw = 2 / 3;
					} else if (end_height == layer.floor_num) {
						start_draw = 1 / 3;
						end_draw = 1;
					} else if (Math.min(start_height, end_height) < layer.floor_num
							&& Math.max(start_height, end_height) > layer.floor_num) {
						start_draw = 1 / 4;
						end_draw = 3 / 4;
					} else {
						return;
					}
				}
				ctx.strokeStyle = style;
				ctx.globalCompositeOperation = 'source-over';
				ctx.lineWidth = lineWidth;
				ctx.lineCap = lineCap;
				ctx.lineJoin = 'round';
				ctx.beginPath();
				tracePath(ctx, anchor, f.geometry.coordinates, start_draw, end_draw);
				ctx.stroke();
			}
		});

		// fill elevator;
		links.forEach(function(f) {
			if (f.properties.route_type != 4 && f.properties['経路の種類'] != '10') {
				return;
			}
			var s = nodeMap[getStartNode(f)], t = nodeMap[getEndNode(f)];
			if (s && t && (getHeight(s) == layer.floor_num || getHeight(t) == layer.floor_num)) {
				var ls = node2xy(s, anchor);
				ctx.globalCompositeOperation = 'source-over';
				ctx.fillStyle = 'rgb(255, 255, 0)';
				ctx.beginPath();
				ctx.moveTo(ls.x, ls.y);
				ctx.arc(ls.x, ls.y, 2, 0, Math.PI * 2);
				ctx.fill();
			}
		});

		// fill no-localization area
		fillArea(1, 'black');
	}

	function tracePath(ctx, anchor, vertices, start_draw, end_draw) {
		function path2array(anchor, vertices) {
			return vertices.map(function(vertex) {
				return latlon2xy({
					'lon' : vertex[0],
					'lat' : vertex[1]
				}, anchor);
			});
		}

		function fixXY(xy, ref, dist) {
			var ratio = dist / Math.sqrt(Math.pow(xy.x - ref.x, 2) + Math.pow(xy.y - ref.y, 2));
			xy.x += (ref.x - xy.x) * ratio;
			xy.y += (ref.y - xy.y) * ratio;
		}

		var array = path2array(anchor, vertices);
		if (end_draw) {
			var dist = [], total = 0;
			for (var i = 1; i < array.length; i++) {
				var len = Math.sqrt(Math.pow(array[i - 1].x - array[i].x, 2) + Math.pow(array[i - 1].y - array[i].y, 2));
				dist.push(len);
				total += len;
			}
			var start_fix = start_draw * total, end_fix = (1 - end_draw) * total;
			while (start_fix > 0 && start_fix >= dist[0]) {
				array.shift();
				start_fix -= dist.shift();
			}
			while (end_fix > 0 && end_fix >= dist[dist.length - 1]) {
				array.pop();
				end_fix -= dist.pop();
			}
			start_fix > 0 && fixXY(array[0], array[1], start_fix);
			end_fix > 0   && fixXY(array[array.length - 1], array[array.length - 2], end_fix);
		}
		array.forEach(function(xy, index) {
			index == 0 ? ctx.moveTo(xy.x, xy.y) : ctx.lineTo(xy.x, xy.y);
		});
	}

	return {
		'calculateExtent' : calculateExtent,
		'drawBack' : drawBack,
		'drawLinks' : drawLinks
	};
}