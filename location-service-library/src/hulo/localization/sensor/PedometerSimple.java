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

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class PedometerSimple implements Pedometer {

	static final double G = 9.80665; // m/(s*s)
	static final String ACC = "Acc";
	final int freq = 100; // Hz

	List<SensorData> sensors;
	Double nStepsMemo;

	int nAdded = 0;
	int nMemo = 10*freq;
	int tNear = 3*freq;
	SensorData[] sensorDataArray = new SensorData[nMemo];
	int indexPoint=0;

	final int NDIM = 3;

	long[] timestamps;

	static final String WALK_DETECT_STD_WINDOW = "walkDetectStdWindow";
	static final String WALK_DETECT_SIGMA_THRESHOLD = "walkDetectSigmaThreshold";

	int walkDetectStdWindow = 80;
	double walkDetectSigmaThreshold = 0.6;

	public void setWalkDetectStdWindow(int walkDetectStdWindow) {
		this.walkDetectStdWindow = walkDetectStdWindow;
	}
	public void setWalkDetectSigmaThreshold(double walkDetectSigmaThreshold) {
		this.walkDetectSigmaThreshold = walkDetectSigmaThreshold;
	}

	public void settingFromJSON(JSONObject json){
		try {
			setWalkDetectSigmaThreshold(json.getDouble(WALK_DETECT_SIGMA_THRESHOLD));
			setWalkDetectStdWindow(json.getInt(WALK_DETECT_STD_WINDOW));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private int moveAveWin_Simple_SC = 10;
	private double peak_thresh_SC = 0.2;

	private final String MOVE_AVE_WIN_SC_SIMPLE = "moveAveWin_Simple_SC";
	private final String PEAL_THRESH_SC_SIMPLE = "peak_thresh_SC";

	public void setSensorDataList(List<SensorData> sensors){
		nStepsMemo = null;
		this.sensors = sensors;
	}

	public double getNSteps(){
		if(nStepsMemo==null){
			nStepsMemo = getNSteps(sensors);
			sensors = null;
		}
		return nStepsMemo;
	}

	public double getNSteps(List<SensorData> sensorDataList){
		return estimateNSteps(sensorDataList);
	}


	public double estimateNSteps(List<SensorData> sensorDataList){
		List<SensorData> accList = SensorData.filter(sensorDataList, ACC);
		for(SensorData sd: accList){
			addBuffer(sd);
		}

		SensorData[] sds = getNearSensorData();
		double[] amps = computeAmplitude(sds, G);

		amps = SensorUtils.subtract(amps, 1.0);

		double[] devs = SensorUtils.computeSlidingWindowDeviation(amps, walkDetectStdWindow);

		double[] maAmps = SensorUtils.computeMovingAverage(amps, moveAveWin_Simple_SC);
		double[] overThresh = SensorUtils.thresholding(maAmps, peak_thresh_SC);
		double[] steps = increaseStep(overThresh);
		double[] cumSteps = SensorUtils.accumulate(steps);
		double[] interpolated = SensorUtils.linearInterpolate(cumSteps);

		int i_start = cumSteps.length - freq >=0 ? cumSteps.length - freq : 0;
		int i_end = cumSteps.length-1;
		double nStep = interpolated[i_end] - interpolated[i_start];

		return nStep;
	}

	void addBuffer(SensorData sd){
		indexPoint = indexPoint % nMemo;
		sensorDataArray[indexPoint] = sd;
		indexPoint++;
		nAdded ++;
	}

	SensorData getBuffer(int index){
		if(index>0){
			System.out.println("index >0 is not supported.");
			return null;
		}else if( Math.abs(index) > nAdded ){
			return null;
		}else{
			return sensorDataArray[(indexPoint + index + nMemo) % nMemo];
		}
	}

	public SensorData[] getNearSensorData(){
		List<SensorData> sds = new ArrayList<SensorData>();
		for(int i=-tNear; i<=0; i++){
			SensorData sd = getBuffer(i);
			if(sd != null){
				sds.add(sd);
			}
		}
		return sds.toArray(new SensorData[0]);
	}

	public double[] computeAmplitude(SensorData[] sds, double unit){
		int n = sds.length;
		double[] amps = new double[n];
		for(int i=0; i<n; i++){
			double sqsum = 0;
			double[] data = sds[i].getData();
			for(int j=0; j<data.length; j++){
				double a = data[j]*unit;
				sqsum += a*a;
			}
			amps[i] = Math.sqrt(sqsum);
		}
		return amps;
	}

	double[] increaseStep(double[] x){
		double[] dx = SensorUtils.derivative(x);
		double[] incs = new double[dx.length];

		for(int i=0; i<incs.length; i++){
			incs[i] = dx[i]>0? 1:0;
		}
		return incs;
	}

	public String getName(){
		return this.getClass().getSimpleName();
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getName());
		sb.append("(");
		sb.append(WALK_DETECT_STD_WINDOW +"="+ walkDetectStdWindow);
		sb.append(",");
		sb.append(WALK_DETECT_SIGMA_THRESHOLD +"="+ walkDetectSigmaThreshold);
		sb.append(",");
		sb.append(MOVE_AVE_WIN_SC_SIMPLE +"=" + moveAveWin_Simple_SC);
		sb.append(",");
		sb.append(PEAL_THRESH_SC_SIMPLE + "=" + peak_thresh_SC);
		sb.append(")");
		return sb.toString();
	}
}