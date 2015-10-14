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

import hulo.localization.map.MapFloorsModel;
import hulo.localization.models.ModelBuilderCore;
import hulo.localization.project.ProjectData;

import java.util.HashSet;
import java.util.Set;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class RandomWalkerModelFactory implements SystemModelFactory {

	public static final String RANDOMWALKFREE = "RandomWalkerOnFloorsFree";
	public static final String RANDOM_WALKER_ON_FLOORS_FREE_WITH_SENSOR = "RandomWalkerOnFloorsFreeWithSensor";
	public static final String RANDOM_WALKER_ON_FLOORS_MAP = "RandomWalkerOnFloorsMap";
	public static final String RANDOM_WALKER_WITH_SENSOR = "RandomWalkerWithSensor";

	public static final String POSE_RANDOM_WALKER = PoseRandomWalker.class.getSimpleName();

	static Set<String> nameSet = new HashSet<>();
	static{
		nameSet.add(RANDOMWALKFREE);
		nameSet.add(RANDOM_WALKER_ON_FLOORS_FREE_WITH_SENSOR);
		nameSet.add(RANDOM_WALKER_ON_FLOORS_MAP);
		nameSet.add(RANDOM_WALKER_WITH_SENSOR);
		nameSet.add(POSE_RANDOM_WALKER);
	}

	@Override
	public boolean hasModel(JSONObject json) {
		try{
			String name = json.getString("name");
			if(nameSet.contains(name)){
				return true;
			}
		}catch(JSONException e){
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public SystemModel create(JSONObject json, ProjectData projData) {
		SystemModel sysModel = null;
		try {
			String name = json.getString("name");
			System.out.println("Building a system model : "+name);
			JSONObject paramsJSON = json.getJSONObject("parameters");
			if(name.equals(RANDOMWALKFREE)){
				RandomWalkerOnFloorsFree rw = new RandomWalkerOnFloorsFree();
				rw.settingFromJSON(paramsJSON);
				sysModel = rw;
			}
			else if(name.equals(RANDOM_WALKER_ON_FLOORS_FREE_WITH_SENSOR)){
				RandomWalkerOnFloorsFreeWithSensor rw = new RandomWalkerOnFloorsFreeWithSensor();
				rw.settingFromJSON(paramsJSON);
				sysModel = rw;
			}
			// with map
			else if(name.equals(RANDOM_WALKER_ON_FLOORS_MAP)){
				RandomWalkerOnFloorsMap rw = new RandomWalkerOnFloorsMap();
				MapFloorsModel mapFloorsModel = (MapFloorsModel) projData.mapData().getObjects().get(MapFloorsModel.MAP_FLOORS_MODEL);
				rw.settingFromJSON(paramsJSON);
				rw.setMapFloorsModel(mapFloorsModel);
				sysModel = rw;
			}
			else if(name.equals(RANDOM_WALKER_WITH_SENSOR)){
				RandomWalkerWithSensor rw = new RandomWalkerWithSensor();
				SystemModel rwInner = ModelBuilderCore.buildSystemModel(paramsJSON, projData);
				if(rwInner instanceof RandomWalker){
					rw.setRandomWalker((RandomWalker) rwInner);
				}else{
					System.out.println("rwInner must be RandomWalker type.");
				}
				rw.settingFromJSON(paramsJSON);
				sysModel = rw;
			}
			else if(name.equals(POSE_RANDOM_WALKER)){
				PoseRandomWalker rw = new PoseRandomWalker();
				rw.settingFromJSON(paramsJSON);
				sysModel = rw;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
		return sysModel;
	}

}
