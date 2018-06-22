

function latlon2mercator(latlon) {
  var xy = new OpenLayers.LonLat(latlon.lon, latlon.lat);
  xy = xy.transform('EPSG:4326', 'EPSG:3857');
  return {x:xy.lon, y:xy.lat};
}

function mercator2latlon(mercator) {
  var latlon = new OpenLayers.Geometry.Point(mercator.x, mercator.y);
  latlon = latlon.transform('EPSG:3857', 'EPSG:4326');
  return {lon:latlon.x, lat:latlon.y};
}

function getPointResolution(anchor) {
	var p1 = new OpenLayers.Geometry.Point(anchor.x, anchor.y);
	var p2 = new OpenLayers.Geometry.Point(anchor.x+1, anchor.y);
	return (new OpenLayers.Geometry.LineString([p1, p2])).getGeodesicLength('EPSG:3857');
}


function mercator2xy(src_mercator, anchor) {
  var r = getPointResolution(anchor);
  var dx = (src_mercator.x - anchor.x) * r;
  var dy = (src_mercator.y - anchor.y) * r;
  var rad = anchor.rotate / 180 * Math.PI;
  var c = Math.cos(rad);
  var s = Math.sin(rad);
  var x = dx * c - dy * s;
  var y = dx * s + dy * c;
  return {x:x, y:y};
}

function xy2mercator(src_xy, anchor) {
  var r = getPointResolution(anchor);
  var x = src_xy.x;
  var y = src_xy.y;
  var rad = - anchor.rotate / 180 * Math.PI;
  var c = Math.cos(rad);
  var s = Math.sin(rad);
  var dx = (x * c - y * s) / r;
  var dy = (x * s + y * c) / r;
  return {x:anchor.x + dx, y:anchor.y + dy};
}

function latlon2xy(latlon, anchor) {
  var temp = latlon2mercator(anchor);
  anchor.x = temp.x;
  anchor.y = temp.y;
  var mer = latlon2mercator(latlon);
  var xy = mercator2xy(mer, anchor);
  return xy;
}

function xy2latlon(xy, anchor) {
  var temp = latlon2mercator(anchor);
  anchor.x = temp.x;
  anchor.y = temp.y;
  var mer = xy2mercator(xy, anchor);
  var latlon = mercator2latlon(mer);
  return latlon;
}

function toDegrees(angle) {
	return angle*(180/Math.PI);
}

function toRadians(angle) {
	return angle*(Math.PI/180);
}

function headingGlobalToLocal(heading, anchor) {
    var localHeading = toRadians(heading - anchor.rotate);
    var xH = Math.sin(localHeading);
    var yH = Math.cos(localHeading);
    var orientation = toDegrees(Math.atan2(yH,xH));
    return orientation;
}

function headingLocalToGlobal(heading, anchor) {
    var radHeading = toRadians(heading);
    var xH = Math.sin(radHeading);
    var yH = Math.cos(radHeading);
    var orientation = toDegrees(Math.atan2(yH,xH)) + anchor.rotate;
    return orientation;
}
