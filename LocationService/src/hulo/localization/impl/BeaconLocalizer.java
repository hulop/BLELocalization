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

import hulo.localization.Beacon;
import hulo.localization.DataManager;
import hulo.localization.LocalizationInput;
import hulo.localization.LocalizationInput.UpdateMode;
import hulo.localization.LocalizationService;
import hulo.localization.LocalizationStatus;
import hulo.localization.data.DataUtils;
import hulo.localization.sensor.SensorData;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class BeaconLocalizer {

	private static JSONObject settings;
	private static DataManager mDataManager;
	private static LocalizationService mLocalizationService;

	private static final String DEFAULT_LOCATION_SERVICE_CONFIG_FILE_PATH = "/hulo/localization/resource/settings/deployment.json";

	static{
		String settingPath = System.getenv("LOCATION_SERVICE_CONFIG_FILE_PATH");
		if (settingPath == null || settingPath.length() == 0) {
			settingPath = DEFAULT_LOCATION_SERVICE_CONFIG_FILE_PATH;
		}
		URL url = BeaconLocalizer.class.getResource(settingPath);
		System.out.println("settingPath URL="+url);
		try {
			InputStream in = BeaconLocalizer.class.getResourceAsStream(settingPath);
			settings = new JSONObject(in);
			mDataManager = new DataManagerImpl(settings);
			mLocalizationService = new LocalizationServiceImpl(mDataManager);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public static void init() {
		// dummy;
	}

	public static void destroy() {
		if (mLocalizationService != null) {
			mLocalizationService.destroy();
		}
	}

	private LocalizationStatus mLocalizationStatus = null;

	public JSONObject reset(JSONObject source) throws JSONException {
		LocalizationInput locInput = toResetLocalizationInput(source);
		mLocalizationStatus = mLocalizationService.update(mLocalizationStatus, locInput);

		if (mLocalizationStatus == null) {
			return (JSONObject) JSON.parse("{\"result\":true}");
		}

		String site_id = mDataManager.getSite(locInput);
		return mLocalizationStatus.toJSONObject().put("site_id", site_id);
	}


	public JSONObject correct(JSONObject source) throws JSONException {
		LocalizationInput locInput = toCorrectLocalizationInput(source);
		mLocalizationStatus = mLocalizationService.update(mLocalizationStatus, locInput);

		if (mLocalizationStatus == null) {
			return (JSONObject) JSON.parse("{\"result\":true}");
		}

		String site_id = mDataManager.getSite(locInput);
		return mLocalizationStatus.toJSONObject().put("site_id", site_id);
	}

	private LocalizationInput toResetLocalizationInput(JSONObject source) throws JSONException {
		String uuid = source.getString("uuid");
		LocalizationInput locInput = new LocalizationInput();
		locInput.setUuid(uuid).setUpdateMode(UpdateMode.reset);
		try{
			if(!source.isNull("xtrue") && !source.isNull("ytrue") && !source.isNull("ztrue")){
				double xtrue = source.getDouble("xtrue");
				double ytrue = source.getDouble("ytrue");
				double ztrue = source.getDouble("ztrue");
				locInput.setInitialLocation(xtrue, ytrue, ztrue);
			}
			if(!source.isNull("theta")){
				double theta = source.getDouble("theta");
				locInput.setInitialOrientation(theta);
			}
		} catch(JSONException e){
			e.printStackTrace();
		}
		return locInput;
	}

	private LocalizationInput toCorrectLocalizationInput(JSONObject source) throws JSONException {
		String uuid = source.getString("uuid");
		LocalizationInput locInput = new LocalizationInput();
		locInput.setUuid(uuid).setUpdateMode(UpdateMode.correct);
		try{
			double xtrue = source.getDouble("xtrue");
			double ytrue = source.getDouble("ytrue");
			double ztrue = source.getDouble("ztrue");
			locInput.setInitialLocation(xtrue, ytrue, ztrue);
			double xstdev = source.getDouble("stdevxy");
			double ystdev = xstdev;
			double zstdev = 0.0;
			locInput.setStdevLocTrue(xstdev, ystdev, zstdev);
		} catch(JSONException e){
			e.printStackTrace();
		}
		return locInput;
	}


	public JSONObject update(JSONArray locations) throws JSONException {
		if (locations == null || locations.isEmpty()) {
			return null;
		}
		LocalizationInput locInput = toLocalizationInput(locations);
		// Update status
		mLocalizationStatus = mLocalizationService.update(mLocalizationStatus, locInput);

		if (mLocalizationStatus == null) {
			return (JSONObject) JSON.parse("{\"error\":\"Service is not ready\"}");
		}

		String site_id = mDataManager.getSite(locInput);
		return mLocalizationStatus.toJSONObject().put("site_id", site_id);
	}

	private static LocalizationInput toLocalizationInput(JSONArray locations) throws JSONException {

		LocalizationInput locInput = new LocalizationInput();
		List<Beacon> beacons = new ArrayList<Beacon>();
		List<SensorData> sensors = new ArrayList<SensorData>();
		for (int beaconIndex = 0; beaconIndex < locations.size(); beaconIndex++) {
			JSONObject beaconInfo = locations.getJSONObject(beaconIndex);
			if (beaconInfo.has("uuid") && beaconInfo.has("data")) {
				String uuid = beaconInfo.getString("uuid");
				locInput.setUuid(uuid);
				JSONArray dataArray = beaconInfo.getJSONArray("data");
				for (int dataIndex = 0; dataIndex < dataArray.size(); dataIndex++) {
					JSONObject beaconData = dataArray.getJSONObject(dataIndex);
					if (beaconData.has("major") && beaconData.has("minor") && beaconData.has("rssi")) {
						beacons.add(new Beacon(beaconData.getInt("major"), beaconData.getInt("minor"), (float) beaconData.getDouble("rssi")));
					}
				}
			}
			if (beaconInfo.has("sensorCSV")) {
				String csv = beaconInfo.getString("sensorCSV");
				if(csv.equals("") || csv==null){
					System.out.println("sensorCSV is not contained in input JSON.");
				}else{
					for(String line: csv.split("\n")) {
						sensors.add(DataUtils.parseSensorData(line));
					}
				}
			}
		}
		locInput.setBeacons(beacons);
		locInput.setSensorDataList(sensors);
		return locInput;
	}
}
