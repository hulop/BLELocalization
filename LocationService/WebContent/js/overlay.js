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
function FloorPlanOverlay(options) {
	this.setOption(options);
	this.dom = null;
}

FloorPlanOverlay.prototype = new google.maps.OverlayView();

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
		// end backward compatibility
	}
}

FloorPlanOverlay.prototype.onAdd = function() {
	var div = document.createElement('div');
	div.style.borderStyle = 'none';
	div.style.borderWidth = '0px';
	div.style.position = 'absolute';

	var img = document.createElement('img');
	img.style.width = '100%';
	img.style.position = 'absolute';
	img.style.opacity = this.opacity || 1.0;
	img.src = this.src;

	div.appendChild(img);
	this.dom = div;

	var panes = this.getPanes();
	panes.overlayLayer.appendChild(div);
};

FloorPlanOverlay.prototype.draw = function() {
	var center = new google.maps.LatLng(this.lat, this.lng);
	
	this.origin_x = this.origin_x || this.width/2;
	this.origin_y = this.origin_y || this.height/2;
	
	var width = this.width / this.ppm_x;
	var height = this.height / this.ppm_y;
	var ox = this.origin_x / this.ppm_x;
	var oy = this.origin_y / this.ppm_y;

	var len1 = Math.sqrt(Math.pow(width - ox, 2) + Math.pow(height - oy, 2));
	var len2 = Math.sqrt(Math.pow(ox, 2) + Math.pow(oy, 2));
	var dne = Math.atan2(width - ox, height - oy);
	var dsw = Math.atan2(-ox, -oy);
	var ne = google.maps.geometry.spherical.computeOffset(center, len1, dne * 180 / Math.PI);
	var sw = google.maps.geometry.spherical.computeOffset(center, len2, dsw * 180 / Math.PI);

	var prj = this.getProjection();
	sw = prj.fromLatLngToDivPixel(sw);
	ne = prj.fromLatLngToDivPixel(ne);
	center = prj.fromLatLngToDivPixel(center);

	var div = this.dom;
	var bw = this.border || 0;

	div.style.left = sw.x - bw + 'px';
	div.style.top = ne.y - bw + 'px';
	div.style.width = (ne.x - sw.x + bw * 2) + 'px';
	div.style.height = (sw.y - ne.y + bw * 2) + 'px';
	div.style.border = bw + "px solid gray";

	// cross browser

	div.style.transformOrigin = div.style.oTransformOrigin = div.style.webkitTransformOrigin = (center.x - sw.x) + "px "
			+ (center.y - ne.y) + "px";
	div.style.transform = div.style.oTransform = div.style.webkitTransform = "rotate(" + this.rotate + "deg)";
};

FloorPlanOverlay.prototype.move = function(options) {
	var center = new google.maps.LatLng(this.lat, this.lng);
	center = google.maps.geometry.spherical.computeOffset(center, options.length, options.direction);
	this.lat = center.lat();
	this.lng = center.lng();
	return center;
}

FloorPlanOverlay.prototype.onRemove = function() {
	this.dom.parentNode.removeChild(this.dom);
	this.dom = null;
};

FloorPlanOverlay.prototype.setMap = (function(_super) {
	return function(map) {
		return _super.call(this, map ? map : null);
	}
})(FloorPlanOverlay.prototype.setMap);