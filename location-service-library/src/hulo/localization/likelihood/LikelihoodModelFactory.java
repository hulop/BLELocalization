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

package hulo.localization.likelihood;

import java.util.ServiceLoader;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class LikelihoodModelFactory {

	private static ServiceLoader<LikelihoodModel> loader = ServiceLoader.load(LikelihoodModel.class);

	public static LikelihoodModel create(JSONObject json){
		LikelihoodModel likelihoodModel = null;
		try {
			if(json==null){
				likelihoodModel = new NormalDist();
			}
			else if(json!=null){
				String name = json.getString("name");
				JSONObject paramsJSON = json.getJSONObject("parameters");

				for(LikelihoodModel lmTemp: loader){
					if(name.equals(lmTemp.getClass().getSimpleName())){
						likelihoodModel = lmTemp;
						likelihoodModel.settingFromJSON(paramsJSON);
						System.out.println(likelihoodModel.toString() + " was created.");
						break;
					}
				}
				if(likelihoodModel==null){
					System.err.println(name+ " is not supported in " + LikelihoodModelFactory.class.getSimpleName() + ".");
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return likelihoodModel;
	}
}
