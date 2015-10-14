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

package hulo.localization.models.sys;

import hulo.localization.LocalizationInput;
import hulo.localization.State;
import hulo.localization.models.sys.RandomWalkerOnFloorsFreeWithSensor.WalkState;
import hulo.localization.sensor.Pedometer;
import hulo.localization.sensor.PedometerFactory;
import hulo.localization.sensor.SensorData;

import java.util.List;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class RandomWalkerWithSensor extends RandomWalker implements SystemModelWithSensor, SystemModel{

	RandomWalker randomWalker = null;
	JSONObject paramsJSON ;

	double sigmaWalk = 0.05;
	double sigmaStay = 1.5;

	Pedometer pedo;

	double smallNSteps = 0.01;

	public void setRandomWalker(RandomWalker rw){
		randomWalker = rw;
	}

	@Override
	public void settingFromJSON(JSONObject json){
		paramsJSON = json;
		try {
			sigmaWalk = json.getDouble("sigmaWalk");
			sigmaStay = json.getDouble("sigmaStay");
			randomWalker.setSigma(sigmaWalk);
			JSONObject pedoParams = json.getJSONObject(SensorData.PEDOMETER);
			pedo = PedometerFactory.create(pedoParams);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public State[] predict(State[] states, LocalizationInput input) {
		List<SensorData> sensors = input.getSensorDataList();
		pedo.setSensorDataList(sensors);
		double nSteps = pedo.getNSteps();
		WalkState walkState = updateWalkState(nSteps);
		if(walkState == WalkState.stay){
			randomWalker.setSigma(sigmaStay);
			randomWalker.setIsWalking(false);
		}else if(walkState == WalkState.walk){
			randomWalker.setSigma(sigmaWalk);
			randomWalker.setIsWalking(true);
		}
		State[] statesNew = randomWalker.predict(states);
		return statesNew;
	}

	WalkState updateWalkState(double nSteps){
		return nSteps<smallNSteps? WalkState.stay : WalkState.walk;
	}

	@Override
	public String toString(){
		return "RandomWalkerWithSensor(sigmaStay="
				+sigmaStay+",sigmaWalk="+sigmaWalk+",RandomWalker="+randomWalker.toString()+")";
	}
}