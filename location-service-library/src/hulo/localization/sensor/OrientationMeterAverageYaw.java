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

package hulo.localization.sensor;

import java.util.ArrayList;
import java.util.List;

public class OrientationMeterAverageYaw implements OrientationMeter {

	List<SensorData> sensors;
	double yawAverage = 0.0;
	boolean isValid = false;

	class Attitude{
		long timestamp;
		double pitch;
		double roll;
		double yaw;
	}

	final String ATT = "Att";
	final String MOTION = "Motion";

	public void setSensorDataList(List<SensorData> sensors){
		this.sensors = sensors;
		isValid = false;
	}

	public double getOrientationMeasured(){
		if(! isValid){
			double ytemp = getOrientationMeasured(sensors);
			if(Double.isNaN(ytemp)){
				// pass
			}else{
				yawAverage = ytemp;
			}
			isValid = true;
		}
		return yawAverage;
	}


	double getOrientationMeasured(List<SensorData> sensorDataList){
		return getYawAverage(sensorDataList);
	}


	double getYawAverage(List<SensorData> sensors){
		List<Attitude> atts = sensorDataToAttitude(sensors);
		double[] yaws = getYaws(atts);
		double yawAve = SensorUtils.computeAngleAverage(yaws);
		return yawAve;
	}

	double[] getYaws(List<Attitude> atts){
		int n = atts.size();
		double yaws[] = new double[n];
		for(int i=0; i<n; i++){
			yaws[i] = atts.get(i).yaw;
		}
		return yaws;
	}


	List<Attitude> sensorDataToAttitude(List<SensorData> sensors){
		List<SensorData> motions = SensorData.filter(sensors, MOTION);
		List<SensorData> atts = SensorData.filter(sensors, ATT);

		if(motions != null){
			return motionToAttitude(motions);
		}else if(atts != null){
			return attToAttitude(atts);
		}
		return null;

	}

	List<Attitude> motionToAttitude(List<SensorData> motions){
		List<Attitude> attitudes = new ArrayList<Attitude>();
		for(SensorData motion: motions){
			long timestamp = motion.getTimestamp();
			double[] data = motion.getData();
			Attitude att = new Attitude();
			att.timestamp = timestamp;
			att.pitch = data[0];
			att.roll = data[1];
			att.yaw = data[2];
			attitudes.add(att);
		}
		return attitudes;
	}

	List<Attitude> attToAttitude(List<SensorData> atts){
		return motionToAttitude(atts);
	}

}
