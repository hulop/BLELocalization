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
import hulo.localization.Location;
import hulo.localization.Pose;
import hulo.localization.PoseMotion;
import hulo.localization.PoseSetting;
import hulo.localization.State;
import hulo.localization.sensor.OrientationMeter;
import hulo.localization.sensor.OrientationMeterFactory;
import hulo.localization.sensor.Pedometer;
import hulo.localization.sensor.PedometerFactory;
import hulo.localization.sensor.SensorData;

import java.util.List;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class PoseRandomWalker extends RandomWalker implements SystemModel, SystemModelWithSensor, PoseSystemModel{

	protected Pedometer pedo = null;
	protected OrientationMeter ori = null;
	protected JSONObject paramsJSON = null;

	protected PoseMotion poseMotion = null;
	protected PoseSetting poseSetting = null;

	@Override
	public void settingFromJSON(JSONObject json){
		paramsJSON = json;
		try {
			double sigma = json.getDouble("sigma");
			setSigma(sigma);

			// Set parameters for sensor data
			JSONObject oriParams = json.getJSONObject(SensorData.ORIENTATION_METER);
			ori = OrientationMeterFactory.create(oriParams);
			JSONObject pedoParams = json.getJSONObject(SensorData.PEDOMETER);
			pedo = PedometerFactory.create(pedoParams);

			// PoseSetting
			JSONObject poseSettingJSON = json.getJSONObject(PoseSetting.SIMPLE_NAME);
			this.poseSetting = PoseSetting.createFromJSON(poseSettingJSON);
			System.out.println("PoseData.setting : "+poseSetting.toString());
			this.poseMotion = PoseMotion.create(poseSetting);

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public State[] predict(State[] states, LocalizationInput input){
		// convert
		List<SensorData> sensors = input.getSensorDataList();
		pedo.setSensorDataList(sensors);
		ori.setSensorDataList(sensors);
		int n= states.length;
		State[] statesNew = new State[n];
		for(int i=0; i<n; i++){
			double nSteps = pedo.getNSteps();
			double yawAve = ori.getOrientationMeasured();
			statesNew[i] = predict(states[i], nSteps, yawAve);
		}
		return statesNew;
	}

	public State toPoseData(State state){
		State stateNew = null;
		if(state instanceof Pose){
			stateNew = state;
		}else if(state instanceof Location){
			stateNew = poseMotion.createFrom((Location) state);
		}
		return stateNew;
	}

	public State predict(State state, double nSteps, double yawAve){
		Pose stateNew = (Pose) toPoseData(state);
		stateNew.setNSteps(nSteps);
		stateNew.setOrientationMeasured(yawAve);

		stateNew = poseMotion.predict(stateNew);
		if(poseSetting.floorMoveMode.equals(PoseSetting.FLOORS_FREE)){
			stateNew = poseMotion.predictZ(stateNew);
		}
		return (State) stateNew;
	}

}
