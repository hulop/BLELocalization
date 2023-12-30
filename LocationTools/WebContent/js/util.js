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

function latlon2mercator(latlon) {
  var mer = ol.proj.transform([latlon.lon, latlon.lat], 'EPSG:4326', 'EPSG:3857');
  return {x:mer[0], y:mer[1]};
}

function mercator2latlon(mercator) {
  var latlon = ol.proj.transform([mercator.x, mercator.y], 'EPSG:3857', 'EPSG:4326');
  return {lon:latlon[0], lat:latlon[1]};
}

function getPointResolution(anchor) {
	return ol.proj.getPointResolution("EPSG:3857", 1, [anchor.x, anchor.y]);
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
