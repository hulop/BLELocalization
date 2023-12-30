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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.sis.distance.DistanceUtils;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

public class SISUtils {

	private double EQUATOR_LUNIT; // units / degree @ equator
	private MathTransform mtL2M, mtM2L;

	public SISUtils() {
		try {
			EQUATOR_LUNIT = DistanceUtils.getHaversineDistance(0, 0, 0, 1);
			System.out.println("EQUATOR_LUNIT: " + EQUATOR_LUNIT);
			CoordinateReferenceSystem crs4326 = CommonCRS.WGS84.geographic();
			CoordinateReferenceSystem crs3857 = CRS.fromWKT(readResource("EPSG-CRS-3857.txt"));
			System.out.println("crs4326: " + crs4326);
			System.out.println("crs3857: " + crs3857);
			mtL2M = CRS.findOperation(crs4326, crs3857, null).getMathTransform();
			mtM2L = CRS.findOperation(crs3857, crs4326, null).getMathTransform();
			System.out.println("mtL2M: " + mtL2M);
			System.out.println("mtM2L: " + mtM2L);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Point2D latlon2mercator(double lat, double lng) {
		try {
			return (Point2D) mtL2M.transform(new DirectPosition2D(lat, lng), null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Point2D mercator2latlon(double x, double y) {
		try {
			return (Point2D) mtM2L.transform(new DirectPosition2D(x, y), null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public double getPointResolution(double lat, double lng) {
		return DistanceUtils.getHaversineDistance(lat, 0, lat, 1) / EQUATOR_LUNIT;
	}

	private String readResource(String name) throws IOException {
		InputStream is = this.getClass().getResourceAsStream(name);
		if (is != null) {
			Reader reader = null;
			try {
				reader = new InputStreamReader(is);
				int length;
				char cbuf[] = new char[4096];
				StringBuilder sb = new StringBuilder();
				while ((length = reader.read(cbuf, 0, cbuf.length)) != -1) {
					sb.append(cbuf, 0, length);
				}
				return sb.toString();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					reader.close();
				}
			}
		}
		return null;
	}

	public static void main(String[] args) {
		SISUtils me = new SISUtils();
		for (double lat = -91; lat <= 91; lat += 1.0) {
			System.out.println(lat + " : " + me.getPointResolution(lat, 0));
		}
		Point2D mer = me.latlon2mercator(35.678114711, 139.7870960086);
		System.out.println(mer);
		Point2D ll = me.mercator2latlon(mer.getX(), mer.getY());
		System.out.println(ll);
	}
}
