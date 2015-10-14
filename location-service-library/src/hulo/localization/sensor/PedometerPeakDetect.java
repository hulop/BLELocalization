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

import java.util.Arrays;
import java.util.List;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class PedometerPeakDetect extends PedometerSimple implements Pedometer {

	static final String PEDOMETER_PEAK_DETECT = "PedometerPeakDetect";
	static final String STEP_COUNT_MOVING_AVERAGE_WINDOW = "stepCountMovingAverageWindow";
	static final String STEP_COUNT_PEAK_WINDOW = "stepCountPeakWindow";

	static final String OPT_INTERPOLATE = "optInterpolate";

	// Default parameters
	int stepCountMovingAverageWindow = 30;
	int stepCountPeakWindow = 60;

	boolean optInterpolate=true;

	double lastInterval = 0;
	boolean isWalking = false;
	double firstEstInterval = 0.6;
	int firstStep = 0;

	JSONObject settingJSON;

	public void settingFromJSON(JSONObject json){
		settingJSON = json;
		super.settingFromJSON(json);
		try{
			setStepCountMovingAverageWindow(json.getInt(STEP_COUNT_MOVING_AVERAGE_WINDOW));
			setStepCountPeakWindow(json.getInt(STEP_COUNT_PEAK_WINDOW));
			optInterpolate = json.containsKey(OPT_INTERPOLATE)? json.getBoolean(OPT_INTERPOLATE) : true;
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void setStepCountMovingAverageWindow(int window){
		this.stepCountMovingAverageWindow = window;
	}

	public void setStepCountPeakWindow(int window){
		this.stepCountPeakWindow = window;
	}

	public void setOptInterpolate(boolean optInterpolate){
		this.optInterpolate = optInterpolate;
	}

	@Override
	public double estimateNSteps(List<SensorData> sensorDataList){
		List<SensorData> accList = SensorData.filter(sensorDataList, ACC);
		for(SensorData sd: accList){
			addBuffer(sd);
		}

		SensorData[] sds = getNearSensorData();

		double[] amps = computeAmplitude(sds, G);
		amps = SensorUtils.subtract(amps, G); // Subtract gravity component

		double[] devs = SensorUtils.computeSlidingWindowDeviation(amps, walkDetectStdWindow);
		double[] isWalkings = SensorUtils.thresholding(devs, walkDetectSigmaThreshold);

		double[] maAmps = SensorUtils.computeMovingAverage(amps, stepCountMovingAverageWindow);
		double[] isAmpPeaks = SensorUtils.findLocalMaximum(maAmps, stepCountPeakWindow);

		double[] steps = detectSteps(isWalkings, isAmpPeaks);
		// get interval of last two steps. last step should be in the last second.
		double interval = detectInterval(steps);

		// if more than two steps are detected
		if (true && interval > 0) {
			double step = 1.0 / interval;
			// adjust first step interval
			step += firstStep / freq / interval;
			step -= firstStep / freq / firstEstInterval;
			firstStep = 0;

			isWalking = true;
			lastInterval = interval;

			return step;
		}

		// adjust last step over estimation
		if (isWalking) {
			int lastStepExtraTime2 = 0;
			for(int i = 0; i < steps.length; i++) {
				if (steps[steps.length-i-1] > 0) {
					lastStepExtraTime2 = i;
					break;
				}
			}
			double s = - 1.0 / lastInterval * (lastStepExtraTime2 - freq) / freq;
			isWalking = false;
			//System.out.println("last: "+s);
			return s;
		}

		double step = SensorUtils.sum(Arrays.copyOfRange(steps, Math.max(0, steps.length-freq), steps.length));
		// detect the first step in the last second
		if (step > 0) {
			for(int i = 0; i < steps.length; i++) {
				if (steps[steps.length-i-1] > 0) {
					firstStep = i;
					break;
				}
			}
			// interpolate with estimated interval
			step += firstStep / (double)freq / firstEstInterval;
		}
		//System.out.println("first / nowalk: "+step);
		return step;
	}

	private double detectInterval(double[] steps) {
		int prev = -1;
		for(int i = steps.length-1; i >= 0; i--) {
			if (steps[i] > 0) {
				if (prev > 0) {
					return (prev-i)/(double)freq;
				}
				if (i > steps.length-stepCountPeakWindow*3) {
					prev = i;
				}
			}
		}
		return 0;
	}

	public void printStatus(String str, double[] x){
		System.out.println(str);
		for(double d: x){
			System.out.print(d+",");
		}
	}

	double[] detectSteps(double[] isWalkings, double[] isAmpPeaks){
		int nWD = isWalkings.length;
		int nSC = isAmpPeaks.length;

		int n = Math.max(nWD, nSC);

		double[] steps = new double[n];

		int delay = 0;
		for(int i=0; i<n; i++){
			double iw = isWalkings[Math.max(0, nWD-i-1)];
			double ia = isAmpPeaks[Math.max(0, nSC-i-1-delay)];
			steps[Math.max(0, n-i-1-delay)] = iw*ia;
		}
		return steps;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(PEDOMETER_PEAK_DETECT);
		sb.append("(");
		sb.append(WALK_DETECT_STD_WINDOW +"="+ walkDetectStdWindow);
		sb.append(",");
		sb.append(WALK_DETECT_SIGMA_THRESHOLD +"="+ walkDetectSigmaThreshold);
		sb.append(",");
		sb.append(STEP_COUNT_MOVING_AVERAGE_WINDOW +"=" + stepCountMovingAverageWindow);
		sb.append(",");
		sb.append(STEP_COUNT_PEAK_WINDOW + "=" + stepCountPeakWindow);
		sb.append(")");
		return sb.toString();
	}

}
