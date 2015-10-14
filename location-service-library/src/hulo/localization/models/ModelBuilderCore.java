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

package hulo.localization.models;

import hulo.localization.models.obs.ObservationModel;
import hulo.localization.models.obs.ObservationModelFactory;
import hulo.localization.models.sys.SystemModel;
import hulo.localization.models.sys.SystemModelFactory;
import hulo.localization.project.ProjectData;

import java.util.ServiceLoader;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;


public class ModelBuilderCore {

	private static ServiceLoader<ObservationModelFactory> obsLoader = ServiceLoader.load(ObservationModelFactory.class);
	private static ServiceLoader<SystemModelFactory> sysLoader = ServiceLoader.load(SystemModelFactory.class);


	public ObservationModel buildModel(JSONObject json, ProjectData projData){
		ObservationModel obs = null ;
		try {
			System.out.println("builded model = "+ json.getString("name"));
			obs= buildObservationModel(json, projData);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obs;
	}

	public static ObservationModel buildObservationModel(JSONObject json, ProjectData projData){
		ObservationModel model=null;
		try {
			String name = json.getString("name");
			System.out.println("Building an observation model : "+name);

			for(ObservationModelFactory obsProv: obsLoader){
				System.out.println("ObservationModelProvider="+obsProv.getClass().getSimpleName());
				if(obsProv.hasModel(json)){
					System.out.println("ObservationModelProvider="+obsProv.getClass().getSimpleName() +" is creating an observation model.");
					model = obsProv.create(json, projData);
				}
			}

			if(model==null){
				System.err.println("Unknown observation model name: "+name);
				throw new RuntimeException();
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return model;
	}

	public static SystemModel buildSystemModel(JSONObject json, ProjectData projData){
		SystemModel sysModel = null;
		try {
			String name = json.getString("name");
			System.out.println("Building a system model : "+name);

			for(SystemModelFactory sysProv: sysLoader){
				System.out.println("SystemModelProvider=" + sysProv.getClass().getSimpleName());
				if(sysProv.hasModel(json)){
					System.out.println("SystemModelProvider=" + sysProv.getClass().getSimpleName() +" is creating a system model.");
					sysModel = sysProv.create(json, projData);
				}
			}
			if(sysModel==null){
				System.err.println("Unknown system model name");
				throw new Exception();
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
		return sysModel;
	}
}