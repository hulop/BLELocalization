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

import java.util.HashMap;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class DataManagerImpl implements DataManager {
	private HashMap<String, String> uuidSiteMap = new HashMap<String, String>();
	private JSONObject settings;

	public DataManagerImpl(JSONObject settings) {
		this.settings = settings;
		if (this.settings == null) {
			return;
		}
		try {
			JSONArray array = settings.getJSONArray("configurations");
			if (array == null) {
				return;
			}
			for (int i = 0; i < array.length(); i++) {
				JSONObject config = array.getJSONObject(i);
				uuidSiteMap.put(config.getString("uuid"), config.getString("site_id"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getSite(LocalizationInput input) {
		String uuid = input.getUuid();
		String site_id = uuidSiteMap.get(uuid);

		return site_id;
	}

	@Override
	public String getProjectsDir() {
		if (settings == null) {
			return null;
		}
		try {
			return settings.getString("projects_path");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getModelsDir() {
		if (settings == null) {
			return null;
		}
		try {
			return settings.getString("models_path");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public JSONArray getConfigs() {
		if (settings == null) {
			return null;
		}
		try {
			return settings.getJSONArray("configurations");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public JSONObject getOptions(){
		if (settings == null) {
			return null;
		}
		try {
			if(settings.containsKey("options")){
				return settings.getJSONObject("options");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

}
