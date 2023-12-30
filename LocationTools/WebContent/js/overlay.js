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

/*
 * FloorPlanOverlay displays an indoor map image on a google map.
 * It calculates bounds of image from parameters.
 * -------------------
 * |               |
 * |   *(origin)   | height
 * |               |
 * -------------------
 * |     width     |
 * 
 * @lat:     latitude of the origin
 * @lng:     longitude of the origin
 * @rotate:  rotate angle of the image (degrees, default 0, clockwise). rotate origin is the origin
 * @width:   pixel width of the image
 * @height:  pixel height of the image
 * @origin_x: x coodinate of the origin point
 * @origin_y: y coodinate of the origin point
 * @ppm_x: pixel per meter for x axis
 * @ppm_y: pixel per meter for y axis 
 * @src:     image src
 * @opacity: opacity of the image (default 1.0)
 * @noborder: true/false (default false)
 */
function FloorPlanOverlay(options, map) {
	var overlay = this;
	this.setOption(options);
	(this.img = new Image()).onload = function() {
		if (overlay.width == 1000 && overlay.height == 1000) {
			overlay.width = overlay.img.width;
			overlay.height = overlay.img.height;
			var max = Math.max(overlay.width, overlay.height);
			overlay.ppm_x = overlay.ppm_y = overlay.ppm * max / 1000;
			overlay.origin_x = max / 2;
			if (overlay.width > overlay.height) {
				overlay.origin_y = overlay.height - max / 2;
			} else {
				overlay.origin_y = max / 2;
			}
			console.log(overlay);
		}
		overlay.canvasLayer = new ol.layer.Image({
			'opacity' : 1.0,
			'visible' : overlay.visible,
			'source' : new ol.source.ImageCanvas({
				// 'canvasFunction' : overlay.canvasFunction,
				'canvasFunction' : function(extent, resolution, pixelRatio, size, projection) {
					return overlay.canvasFunction(extent, resolution, pixelRatio, size, projection);
				},
				'projection' : 'EPSG:3857'
			}),
			'zIndex' : overlay.zIndex
		});
		map.addLayer(overlay.canvasLayer);
	}
}

FloorPlanOverlay.prototype.setOption = function(options) {
	if (options) {
		for ( var key in options) {
			this[key] = options[key];
		}
		// backward compatibility
		if (this.ppm_x == undefined || this.ppm_y == undefined) {
			this.ppm_x = this.ppm;
			this.ppm_y = this.ppm;
		}
		this.origin_x = (this.origin_x != undefined) ? this.origin_x : this.width / 2;
		this.origin_y = (this.origin_y != undefined) ? this.origin_y : this.height / 2;
		// end backward compatibility

		this.canvasLayer && this.canvasLayer.getSource().refresh();
	}
}

FloorPlanOverlay.prototype.show = function(show) {
	this.visible = show;
	if (this.canvasLayer) {
		this.canvasLayer.setVisible(show);
	} else if (show) {
		this.img.src = this.src;
	}
}

FloorPlanOverlay.prototype.canvasFunction = function(extent, resolution, pixelRatio, size, projection) {
	// console.log(arguments);

	var canvas = document.createElement('canvas');
	var context = canvas.getContext('2d');
	canvas.setAttribute('width', size[0]);
	canvas.setAttribute('height', size[1]);

	function getTranslate(xy) {
		return [ size[0] * (xy[0] - extent[0]) / (extent[2] - extent[0]), size[1] * (extent[3] - xy[1]) / (extent[3] - extent[1]) ];
	}

	var overlay = this;
	var dpr = window.devicePixelRatio || 1;
	function getScale(xy) {
		var r = ol.proj.getPointResolution(projection, resolution, xy);
		return [ dpr / r / overlay.ppm_x, dpr / r / overlay.ppm_y ];
	}

	var ref = ol.proj.transform([ this.lng, this.lat ], 'EPSG:4326', 'EPSG:3857');
	var trans = getTranslate(ref);
	var scale = getScale(ref);

	context.save();
	context.translate(trans[0], trans[1]);
	context.rotate(this.rotate * Math.PI / 180);
	context.scale(scale[0], scale[1]);
	context.globalAlpha = overlay.opacity;
	context.drawImage(this.img, -this.origin_x, this.origin_y - this.height, this.width, this.height);
	// context.fillStyle = 'rgba(0,0,255,0.1)';
	// context.fillRect(-this.origin_x, this.origin_y - this.height, this.width,
	// this.height);
	// context.arc(0, 0, 10, 0, 2 * Math.PI, false);
	// context.fillStyle = 'red';
	// context.fill();
	context.restore();
	return canvas;
}
