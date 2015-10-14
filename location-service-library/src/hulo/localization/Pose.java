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

package hulo.localization;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class Pose extends Location implements State, Cloneable{

	static double smallNSteps = 0.01;

	double orientation;
	double stepLength;
	double nSteps;

	double orientationBias;
	double orientationMeasured;

	double velocity;
	double velocityRep;
	double fieldVelocityRate = 1.0;

	public void setFieldVelocityRate(double fieldVelocityRate){
		this.fieldVelocityRate = fieldVelocityRate;
	}

	enum WalkState{stay, walk};

	@SuppressWarnings("unused")
	private Pose(){}

	public Pose(double x, double y, double z){
		super(x,y,z);
	}

	public Pose(double x, double y, double z, double orientation, double orientationBias, double stepLength){
		super(x,y,z);
		this.orientationBias = orientationBias;
		this.orientation = orientation;
		this.stepLength = stepLength;
	}

	public void setOrientationBias(double orientationBias){
		this.orientationBias = orientationBias;
	}

	public double getOrientationBias(){
		return orientationBias;
	}

	public void setOrientationMeasured(double orientationMeasured){
		this.orientationMeasured = orientationMeasured;
		if(hasOrientationInitial){
			orientationBias = orientation - orientationMeasured;
			hasOrientationInitial = false;
		}
	}

	public double getOrientationMeasured(){
		return orientationMeasured;
	}

	boolean hasOrientationInitial = false;

	public void setOrientationInitial(double orientationInitial){
		this.orientation = orientationInitial;
		hasOrientationInitial = true;;
	}

	private void setOrientation(double orientation){
		this.orientation = orientation;
	}

	public double getOrientation(){
		return orientation;
	}

	private void setStepLength(double stepLength){
		this.stepLength = stepLength;
	}

	private double getStepLength(){
		return stepLength;
	}

	public void setNSteps(double nSteps){
		this.nSteps = nSteps;
	}

	public double getNSteps(){
		return nSteps;
	}

	public double getVelocity(){
		return velocity;
	}
	public void setVelocity(double velocity){
		this.velocity = velocity;
	}

	@Override
	public Pose clone(){
		return (Pose) super.clone();
	}

	@Override
	public String toString(){
		return x+","+y+","+z+","+Math.toDegrees(orientation)+","
					+Math.toDegrees(orientationBias)+","+velocity;
	}

	public String toDetailString(){
		return "";
	}

	public static Pose mean(State[] states){
		return mean(states, false);
	}

	public static Pose meanWeighted(State[] states){
		return mean(states, true);
	}

	public static Pose mean(State[] states, boolean isWeighted){
		double x_ave=0;
		double y_ave=0;
		double z_ave=0;
		double h_ave=0;

		double stepLength = 0;
		double nSteps = 0;

		double vx = 0;
		double vy = 0;

		double cosOri = 0;
		double sinOri = 0;

		double cosOriBias = 0;
		double sinOriBias = 0;

		double rssiBias = 0;
		double velocityRep = 0;

		int n= states.length;
		for(int i=0; i<n; i++){
			Pose pose = (Pose) states[i];
			double weight = 1.0/((double) n);
			if(isWeighted){
				weight = pose.getWeight();
			}
			double v = pose.velocity;

			x_ave += weight*states[i].getX();
			y_ave += weight*states[i].getY();
			z_ave += weight*states[i].getZ();
			h_ave += weight*states[i].getH();

			stepLength += weight*pose.stepLength;
			nSteps += weight*pose.nSteps;

			velocityRep += weight*pose.velocityRep;

			double ori = pose.orientation;
			cosOri += weight*Math.cos(ori);
			sinOri += weight*Math.sin(ori);

			vx += weight*v*Math.cos(ori);
			vy += weight*v*Math.sin(ori);

			double oriBias = pose.orientationBias;
			cosOriBias += weight*Math.cos(oriBias);
			sinOriBias += weight*Math.sin(oriBias);

			rssiBias += weight*pose.rssiBias;
		}

		double velocity = Math.sqrt(vx*vx + vy*vy);
		double orientation = Math.atan2(sinOri, cosOri);
		double orientationBias = Math.atan2(sinOriBias,cosOriBias);

		Pose poseMean = new Pose(x_ave,y_ave,z_ave,orientation, orientationBias, stepLength);
		poseMean.nSteps = nSteps;
		poseMean.setH((float)h_ave);

		poseMean.velocity = velocity;
		poseMean.velocityRep = velocityRep;
		poseMean.rssiBias = rssiBias;
		poseMean.setWeight(1.0/((double) n));
		return poseMean;

	}


	public static Location stdVelocityAxis(State[] states){
		Pose meanPose = (Pose) mean(states);
		double angle = meanPose.getOrientation();
		Location stdLocAngle = Location.stdAxis(states, angle);
		return stdLocAngle;
	}

	public JSONObject toJSONObject(){
		JSONObject json = null;
		try{
			Pose p = this;
			json = super.toJSONObject();
			json.put("orientation",p.orientation)
				.put("velocity",p.velocity);
		}catch(JSONException e){
			e.printStackTrace();
		}
		return json;
	}

	public double getVelocityRep() {
		return this.velocityRep;
	}

	public void setVelocityRep(double rep) {
		this.velocityRep = rep;
	}

}
