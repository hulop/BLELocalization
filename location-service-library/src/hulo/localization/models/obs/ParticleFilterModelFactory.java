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

package hulo.localization.models.obs;

import hulo.localization.models.ModelBuilderCore;
import hulo.localization.models.sys.SystemModel;
import hulo.localization.project.ProjectData;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class ParticleFilterModelFactory implements ObservationModelFactory {

	@Override
	public boolean hasModel(JSONObject json) {
		try {
			String name = json.getString("name");
			if(name.equals(ParticleFilter.class.getSimpleName())){
				return true;
			}else if(name.equals(ParticleFilterWithSensor.class.getSimpleName())){
				return true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public ObservationModel create(JSONObject json, ProjectData projData) {
		ObservationModel model=null;
		try{

			String name = json.getString("name");
			JSONObject paramsJSON = json.getJSONObject("parameters");

			JSONObject sysJSON = paramsJSON.getJSONObject(SystemModel.class.getSimpleName());
			SystemModel sysModel =ModelBuilderCore.buildSystemModel(sysJSON, projData);

			JSONObject obsJSON = paramsJSON.getJSONObject(ObservationModel.class.getSimpleName());
			ObservationModel obsModel = ModelBuilderCore.buildObservationModel(obsJSON, projData);

			ParticleFilter pf = null;
			if(name.equals(ParticleFilter.class.getSimpleName())){
				pf = new ParticleFilter(sysModel, obsModel);
			}else if(name.equals(ParticleFilterWithSensor.class.getSimpleName())){
				pf = new ParticleFilterWithSensor(sysModel, obsModel);
			}
			pf.settingFromJSON(paramsJSON);
			pf.train(projData.trainData().getSamples());
			model = pf;

		}catch(JSONException e){
			e.printStackTrace();
		}
		return model;
	}

}
