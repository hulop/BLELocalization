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

package hulo.localization.project;

import hulo.localization.BLEBeacon;
import hulo.localization.beacon.BLEBeaconConfig;
import hulo.localization.data.DataUtils;
import hulo.localization.data.MapDataUtils;
import hulo.localization.map.MapFloorsModel;
import hulo.localization.utils.ResourceUtils;

import java.io.InputStream;
import java.util.List;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class MapDataReaderBase implements MapDataReader {

	String readMethodBeacons = null;
	String readMethodFloors = null;

	ResourceUtils resReader = ResourceUtils.getInstance();

	static final String MAP_DIR = "/map";

	static final String MAP_FLOORS_READ_METHOD = "readMapFloorsModel";

	protected String parseReadMethodBeacons(JSONObject formatJSON){
		String readMethodBeacons = null;
		try {
			if(formatJSON.containsKey("map")){
				readMethodBeacons = formatJSON.getJSONObject("map").getJSONObject("beacons").getString("readMethod");
			}else{
				readMethodBeacons = formatJSON.getJSONObject("beacons").getString("readMethod");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return readMethodBeacons;
	}

	protected String parseReadMethodFloors(JSONObject formatJSON){
		String readMethodFloors = null;
		JSONObject floorsJSON = null;
		try {
			if(formatJSON.containsKey("map")){
				floorsJSON = formatJSON.getJSONObject("map").getJSONObject("floors");
				readMethodFloors = floorsJSON.getString("readMethod");
			}else{
				floorsJSON = formatJSON.getJSONObject("floors");
			}
		} catch (JSONException e) {
			System.out.println("The key for [floors] was not found.");
			return null;
		}
		try {
			readMethodFloors = floorsJSON.getString("readMethod");
		} catch (JSONException e) {
			//e.printStackTrace();
			System.out.println("The key for [readMethod] was not found.");
			return null;
		}
		return readMethodFloors;
	}


	@Override
	public boolean hasReadMethod(JSONObject formatJSON){
		readMethodBeacons = parseReadMethodBeacons(formatJSON);
		readMethodFloors = parseReadMethodFloors(formatJSON);
		if(DataUtils.hasBLEBeaconReadMethod(readMethodBeacons)){
			return true;
		}
		return false;
	}

	@Override
	public MapData read(String projectPath, JSONObject formatJSON) {
		MapData mapData = new MapData();
		mapData = loadMapDataBeacons(mapData, projectPath, formatJSON);
		mapData = loadMapDataFloors(mapData, projectPath, formatJSON);
		return mapData;
	}

	protected MapData loadMapDataBeacons(MapData mapData, String projectPath, JSONObject formatJSON){
		mapData = loadMapDataBeaconsLocations(mapData, projectPath, formatJSON);
		mapData = loadMapDataBeaconsConfig(mapData, projectPath, formatJSON);
		return mapData;
	}


	protected MapData loadMapDataBeaconsLocations(MapData mapData, String projectPath, JSONObject formatJSON){
		String beaconsPath = projectPath + MAP_DIR + "/beacons.json";
		InputStream is = resReader.getInputStream(beaconsPath);
		List<BLEBeacon> bleBeacons = DataUtils.readJSONBLEBeaconLocation(is);
		System.out.println("#bleBeacons="+bleBeacons.size());
		mapData.setBLEBeacons(bleBeacons);
		return mapData;
	}

	protected MapData loadMapDataBeaconsConfig(MapData mapData, String projectPath, JSONObject formatJSON){
		String csvBeaconConfig = projectPath + MAP_DIR + "/beacon.csv";
		InputStream is =  resReader.getInputStream(csvBeaconConfig);
		BLEBeaconConfig ibconfig = DataUtils.readCSVBLEBeaconConfiguration(is);
		ibconfig.assignPropertiesToBLEBeacons(mapData.getBLEBeacons());
		System.out.println("BLEBeaconConfig assigned outputPower and msdPower to BLEBeacons.");
		return mapData;
	}

	protected MapData loadMapDataFloors(MapData mapData, String projectPath, JSONObject formatJSON){
		String readMethod = readMethodFloors;
		String mapDir = projectPath + MAP_DIR;
		if(readMethod != null){
			if(readMethod.equals(MAP_FLOORS_READ_METHOD)){
				MapFloorsModel mfModel = MapDataUtils.readMapFloorsModel(mapDir);
				mapData.getObjects().put(MapFloorsModel.MAP_FLOORS_MODEL, mfModel);
			}
		}
		return mapData;
	}


	protected void displayDistanceStatistics(List<BLEBeacon> bleBeacons){
		int n = bleBeacons.size();
		double[][] dists = new double[n][n];
		double[][] fDiffs = new double[n][n];

		for(int i=0; i<n; i++){
			for(int j=i; j<n; j++){
				double d = bleBeacons.get(i).getDistance(bleBeacons.get(j));
				dists[i][j] = d;
				dists[j][i] = d;
				double fDiff = bleBeacons.get(i).getFloorDifference(bleBeacons.get(j));
				fDiffs[i][j] = fDiff;
				fDiffs[j][i] = fDiff;
			}
		}

		double[] minDists = new double[n];

		for(int i=0; i<n; i++){
			minDists[i] = Double.MAX_VALUE;
			for(int j=0; j<n; j++){
				if(i!=j && fDiffs[i][j]==0){
					minDists[i] = Math.min(minDists[i], dists[i][j]);
				}
			}
		}

		double minDist = Double.MAX_VALUE;
		double maxDist = -Double.MIN_VALUE;

		for(int i=0; i<n; i++){
			minDist = Math.min(minDist, minDists[i]);
			maxDist = Math.max(maxDist, minDists[i]);
		}
		System.out.println("min(min(dists))="+minDist+",max(min(dists))="+maxDist);
	}

}
