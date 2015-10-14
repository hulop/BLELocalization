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

package hulo.localization.sensor;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;


public class PedometerFactory {

	static final String SIMPLE = "PedometerSimple";
	static final String DUMMY = "PedometerDummy";
	static final String PEAK_DETECT = "PedometerPeakDetect";
	static final String WALK_STATE = "PedometerWalkState";

	static final String DIRECT = "PedometerDirect";

	public static Pedometer create(JSONObject pedometerParams){
		String name;
		try {
			name = pedometerParams.getString("name");
			Pedometer pedo = null;
			JSONObject paramsJSON = null;
			if(name.equals(DUMMY)){
				pedo = new PedometerRandom();
			}else if(name.equals(SIMPLE)){
				PedometerSimple pedoTmp = new PedometerSimple();
				paramsJSON = pedometerParams.getJSONObject("parameters");
				pedoTmp.settingFromJSON(paramsJSON);
				System.out.println(pedoTmp.toString());
				pedo = pedoTmp;
			}else if(name.equals(PEAK_DETECT)){
				PedometerPeakDetect pedoTmp = new PedometerPeakDetect();
				paramsJSON = pedometerParams.getJSONObject("parameters");
				pedoTmp.settingFromJSON(paramsJSON);
				System.out.println(pedoTmp.toString());
				pedo = pedoTmp;
			}else if(name.equals(DIRECT)){
				PedometerDirect pedoTmp = new PedometerDirect();
				System.out.println(pedoTmp.toString());
				pedo = pedoTmp;
			}
			else{
				paramsJSON = pedometerParams.getJSONObject("parameters");
				String fullName = "hulo.localization.sensor."+name;
				pedo = (Pedometer) Class.forName(fullName).newInstance();
				pedo.settingFromJSON(paramsJSON);
				System.out.println(pedo.toString());
			}
			System.out.println(name + " was created.");
			return pedo;
		} catch (JSONException e) {
			e.printStackTrace();
		} catch( ClassNotFoundException e){
			e.printStackTrace();
		} catch (InstantiationException |  IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
