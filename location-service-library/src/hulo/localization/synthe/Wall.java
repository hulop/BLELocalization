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

package hulo.localization.synthe;

import hulo.localization.State;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public class Wall {

	Line2D line;
	double height;
	double attenuation;

	static final double MAX_HEIGHT = 5.0;
	static final double DEFAULT_ATTENUATION = -10;

	double getHeight(){
		return height;
	}

	double getAttenuation(){
		return attenuation;
	}

	Line2D getLine2D(){
		return line;
	}

	private Wall(){}

	private Wall(double x1, double y1, double x2, double y2){
		line = new Line2D.Double(x1, y1, x2, y2);
	}

	Point2D locationToPoint2D(State state){
		double x = state.getX();
		double y = state.getY();
		Point2D p = new Point2D.Double(x, y);
		return p;
	}

	public double computeNumIntersect(State locTrans, State locReceive){
		Point2D pT = locationToPoint2D(locTrans);
		Point2D pR = locationToPoint2D(locReceive);
		Line2D lineOfSight = new Line2D.Double(pT, pR);
		int count = 0;
		Line2D wallLine = this.getLine2D();
		if(lineOfSight.intersectsLine(wallLine)){
			count ++ ;
		}
		return count;
	}

	public double computeAttenuation(State locTrans, State locReceive){
		return attenuation*this.computeNumIntersect(locTrans, locReceive);
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Wall "+line2DToString(line) + ",height="+height+",attenuation="+attenuation);
		return sb.toString();
	}

	String line2DToString(Line2D line){
		StringBuilder sb = new StringBuilder();
		sb.append("x1="+line.getX1()+",");
		sb.append("y1="+line.getY2()+",");
		sb.append("x2="+line.getX2()+",");
		sb.append("y2="+line.getY2());
		return sb.toString();
	}


	public static class Builder{

		Line2D line;
		double height = MAX_HEIGHT;
		double attenuation = DEFAULT_ATTENUATION;

		Builder(double x1, double y1, double x2, double y2){
			line = new Line2D.Double(x1, y1, x2, y2);
		}

		Builder height(double height){
			this.height = height;
			return this;
		}

		Builder attenuation(double attenuation){
			this.attenuation = attenuation;
			return this;
		}

		public Wall build(){
			Wall wall = new Wall();
			wall.line = this.line;
			wall.height = this.height;
			wall.attenuation = this.attenuation;
			return wall;
		}

	}

}
