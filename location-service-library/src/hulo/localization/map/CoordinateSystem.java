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

package hulo.localization.map;

import hulo.localization.State;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class is used to handle coordinate transformation.
 */
public class CoordinateSystem{
	/*
	 * Class to manage coordinate transformation
	 *   x_local(pixel) = punit_x(pixel/m) * x_world(m) + x_origin(pixel)
	 *   y_local = punit_y * y_world + y_origin
	 *   z_local = punit_z * z_world + z_origin
	 */

	double punit_x = 1;
	double punit_y = 1;
	double punit_z = 1;
	double x_origin = 0;
	double y_origin = 0;
	double z_origin = 0;

	static final String PUNITX = "punit_x";
	static final String PUNITY = "punit_y";
	static final String PUNITZ = "punit_z";
	static final String XORIGIN = "x_origin";
	static final String YORIGIN = "y_origin";
	static final String ZORIGIN = "z_origin";

	public CoordinateSystem(){

	}

	public CoordinateSystem(InputStream is){
		setUpByPropertiesFile(is);
	}

	private void setUpByPropertiesFile(InputStream is){
		Properties config = new Properties();
		try {
			config.load(is);
			punit_x = Double.parseDouble(config.getProperty(PUNITX));
			punit_y = Double.parseDouble(config.getProperty(PUNITY));
			punit_z = Double.parseDouble(config.getProperty(PUNITZ));
			x_origin = Double.parseDouble(config.getProperty(XORIGIN));
			y_origin = Double.parseDouble(config.getProperty(YORIGIN));
			z_origin = Double.parseDouble(config.getProperty(ZORIGIN));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public CoordinateSystem(double punit_x, double punit_y, double x_origin, double y_origin){
		setPunit(punit_x, punit_y);
		setOrigin(x_origin, y_origin);
	}

	public CoordinateSystem(double punit_x, double punit_y, double punit_z,
					double x_origin, double y_origin, double z_origin){
		setPunit(punit_x, punit_y, punit_z);
		setOrigin(x_origin, y_origin, z_origin);
	}

	public State worldToLocalState(State state){
		double x_local = punit_x * state.getX() + x_origin;
		double y_local = punit_y * state.getY() + y_origin;
		double z_local = punit_z * state.getZ() + z_origin;
		State stateNew = (State) state.clone();
		stateNew.setX((float) x_local);
		stateNew.setY((float) y_local);
		stateNew.setZ((float) z_local);
		return stateNew;
	}

	public State localToWorldState(State locCoord){
		double x_world = (locCoord.getX() - x_origin)/punit_x;
		double y_world = (locCoord.getY() - y_origin)/punit_y;
		double z_world = (locCoord.getZ() - z_origin)/punit_z;
		State stateNew = (State) locCoord.clone();
		stateNew.setX((float) x_world);
		stateNew.setY((float) y_world);
		stateNew.setZ((float) z_world);
		return stateNew;
	}

	public void setPunit(double punit_x, double punit_y){
		setPunit(punit_x, punit_y, 1.0);
	}
	public void setPunit(double punit_x, double punit_y, double punit_z){
		this.punit_x = punit_x;
		this.punit_y = punit_y;
		this.punit_z = punit_z;
	}

	public void setOrigin(double x_origin, double y_origin){
		setOrigin(x_origin, y_origin, 0.0);
	}
	public void setOrigin(double x_origin, double y_origin, double z_origin){
		this.x_origin = x_origin;
		this.y_origin = y_origin;
		this.z_origin = z_origin;
	}
}