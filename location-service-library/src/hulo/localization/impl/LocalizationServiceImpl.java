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

package hulo.localization.impl;

import hulo.localization.DataManager;
import hulo.localization.LocalizationInput;
import hulo.localization.LocalizationService;
import hulo.localization.LocalizationStatus;
import hulo.localization.extention.PostProcessor;
import hulo.localization.extention.PostProcessorProvider;
import hulo.localization.models.obs.ObservationModel;
import hulo.localization.thread.ExecutorServiceHolder;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;


public class LocalizationServiceImpl implements LocalizationService{
	private DataManager manager;
	HashMap<String, ObservationModel> modelMap;
	HashSet<String> modelInitializedMap;

	HashMap<String, PostProcessor> postProcessMap;

	public LocalizationServiceImpl(DataManager manager) {
		this.manager = manager;
		this.modelMap = new HashMap<String, ObservationModel>();
		this.modelInitializedMap = new HashSet<String>();
		this.postProcessMap = new HashMap<String, PostProcessor>();

		final JSONObject options = manager.getOptions();

		new Thread(new Runnable(){
			public void run() {
				JSONArray configs = LocalizationServiceImpl.this.manager.getConfigs();
				for(int i = 0; i < configs.length(); i++) {
					try {
						final JSONObject config = configs.getJSONObject(i);
						if (config != null) {
							String site_id = config.getString("site_id");
							ObservationModel obsModel = instantiateModel(config, options);
							if(obsModel!=null){
								modelMap.put(site_id, obsModel);
								System.out.println("Localization model for "+site_id+" is loaded");
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	private ObservationModel instantiateModel(JSONObject config, JSONObject options){
		System.out.println("Initializing a localization model.");

		// Parse configs
		String modelID = null, projectName = null, site_id = null;
		try {
			modelID = config.getString("model");
			projectName = config.getString("project");
			site_id = config.getString("site_id");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (modelID == null || projectName == null || site_id == null) {
			System.err.println("model,  project, and site_id should be specified");
			System.err.println(config.toString());
			return null;
		}

		// Build an observation model.
		String projectPath = manager.getProjectsDir()+"/"+projectName;
		String modelPath = manager.getModelsDir()+"/"+modelID;

		ObservationModelBuilder builder = new ObservationModelBuilder();
		if(options!=null){
			builder.setEnvironmentOption(options);
		}
		builder.setProjectPathAndModelPath(projectPath, modelPath).printStatus();
		builder.setReadsTest(false).loadProject().loadModel();
		ObservationModel observationModel = builder.getObservationModel();

		// Set up post processor if necessary.
		PostProcessor postProc = PostProcessorProvider.create(builder, config, options);
		if(postProc!=null){
			postProcessMap.put(site_id, postProc);
		}

		return observationModel;
	}

	@Override
	public LocalizationStatus update(LocalizationStatus status, LocalizationInput input){
		String currentSite = manager.getSite(input);
		if (currentSite == null) {
			return null;
		}
		ObservationModel observationModel = modelMap.get(currentSite);
		if (observationModel == null) {
			return null;
		}
		LocalizationStatus statusUpdated = observationModel.update(status, input);

		if(postProcessMap.containsKey(currentSite)){
			if(postProcessMap.get(currentSite)!=null){
				statusUpdated = postProcessMap.get(currentSite).update(statusUpdated, input);
			}
		}

		return statusUpdated;
	}

	@Override
	public void destroy() {
		ExecutorServiceHolder.destroy();
	}
}
