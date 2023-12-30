/*******************************************************************************
 * Copyright (c) 2014, 2023  IBM Corporation, Carnegie Mellon University and others
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

package hulo.model.utils;

import java.awt.geom.Point2D;

public class GeoUtils {

	private static final SISUtils sis = new SISUtils();
	// private static final double EQUATOR_LUNIT = WebMercator.xToLongitude(1); //
	// longitude of unit @ equator

	private static double getPointResolution(Point2D anchor) {
		return sis.getPointResolution(anchor.getX(), anchor.getY());
		// double lat = anchor.getY();
		// double dist = Distance.getDistance(lat, 0, lat, EQUATOR_LUNIT);
		// return Double.isNaN(dist) ? 1 : dist;
	}

	private static Point2D latlon2mercator(Point2D latlon) {
		return sis.latlon2mercator(latlon.getX(), latlon.getY());
		// double x = WebMercator.longitudeToX(latlon.getX());
		// double y = WebMercator.latitudeToY(latlon.getY());
		// return new Point2D.Double(x, y);
	}

	private static Point2D mercator2latlon(Point2D mercator) {
		return sis.mercator2latlon(mercator.getX(), mercator.getY());
		// double lng = WebMercator.xToLongitude(mercator.getX());
		// double lat = WebMercator.yToLatitude(mercator.getY());
		// return new Point2D.Double(lng, lat);
	}

	private static Point2D mercator2xy(Point2D src_mercator, Point2D anchor, double anchor_rotate) {
		double r = getPointResolution(anchor);
		Point2D anchor_mercator = latlon2mercator(anchor);
		double dx = (src_mercator.getX() - anchor_mercator.getX()) * r;
		double dy = (src_mercator.getY() - anchor_mercator.getY()) * r;
		double rad = anchor_rotate / 180 * Math.PI;
		double c = Math.cos(rad);
		double s = Math.sin(rad);
		double x = dx * c - dy * s;
		double y = dx * s + dy * c;
		return new Point2D.Double(x, y);
	}

	private static Point2D xy2mercator(Point2D src_xy, Point2D anchor, double anchor_rotate) {
		double r = getPointResolution(anchor);
		Point2D anchor_mercator = latlon2mercator(anchor);
		double x = src_xy.getX();
		double y = src_xy.getY();
		double rad = -anchor_rotate / 180 * Math.PI;
		double c = Math.cos(rad);
		double s = Math.sin(rad);
		double dx = (x * c - y * s) / r;
		double dy = (x * s + y * c) / r;
		return new Point2D.Double(anchor_mercator.getX() + dx, anchor_mercator.getY() + dy);
	}

	public static Point2D latlon2xy(Point2D latlon, Point2D anchor, double anchor_rotate) {
		return mercator2xy(latlon2mercator(latlon), anchor, anchor_rotate);
	}

	public static Point2D xy2latlon(Point2D xy, Point2D anchor, double anchor_rotate) {
		return mercator2latlon(xy2mercator(xy, anchor, anchor_rotate));
	}
}
