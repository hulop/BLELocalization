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

package hulo.localization.beacon;

import java.util.ServiceLoader;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class BeaconFilterFactory {
	private static ServiceLoader<BeaconFilter> loader = ServiceLoader.load(BeaconFilter.class);

	BeaconFilterFactory(){}

	public static BeaconFilter create(JSONObject json){
		BeaconFilter bf = null;
		try{
			String name = json.getString("name");
			JSONObject params = json.getJSONObject("parameters");
			for(BeaconFilter bfTemp: loader){
				if(name.equals(bfTemp.getClass().getSimpleName())){
					bf = bfTemp;
					bf.settingFromJSON(params);
					break;
				}
			}
			if(bf==null){
				System.err.println(name + " is not supported in "+BeaconFilterFactory.class.getSimpleName()+".");
			}
		}catch(JSONException e){
			e.printStackTrace();
		}
		return bf;
	}
}

