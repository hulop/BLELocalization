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

import hulo.localization.Sample;
import hulo.localization.utils.JSONUtils;

import java.util.List;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;


public class GaussianProcessLDPLMeanMultiModel extends
		GaussianProcessLDPLMeanModel {

	GaussianProcessLDPLMeanMulti gpLDPL;

	boolean optMultiHP = false;

	public GaussianProcessLDPLMeanMultiModel setOptMultiHP(boolean optMultiHP){
		this.optMultiHP = optMultiHP;
		return this;
	}

	public GaussianProcessLDPLMeanMultiModel() {
		super.gpLDPL = new GaussianProcessLDPLMeanMulti();
		gpLDPL = (GaussianProcessLDPLMeanMulti) super.gpLDPL;
	}

	@Override
	public void train(List<Sample> samples){
		super.train(samples);
		if(optMultiHP){
			gpLDPL.optimizeLDPLMultiHyperParams();
		}else{
			gpLDPL.optimizeLDPLMultiParameters();
		}
		super.train(samples);
		super.gpLDPL = gpLDPL;
	}

	public JSONObject toJSON(){
		JSONObject json = super.toJSON();
		try {
			JSONObject LDPLJSON = json.getJSONObject(LDPLParameters);
			double[] lambdas = gpLDPL.getStabilizeParameter();
			LDPLJSON.put("lambdas", new Double[]{lambdas[0],lambdas[1],lambdas[2],lambdas[3]});
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}

	public void settingFromJSON(JSONObject json){
		super.settingFromJSON(json);
		try {
			JSONObject ldplParams = json.getJSONObject(LDPLParameters);
			//LDPLParameters
			if(ldplParams.containsKey("lambdas")){
				JSONArray jarray = ldplParams.getJSONArray("lambdas");
				double[] lambdas = JSONUtils.array1DfromJSONArray(jarray);
				System.out.println("lambdas = "+jarray.toString());
				gpLDPL.setStabilizeParameter(lambdas);
			}else{
				System.out.println("The key for [lambdas] was not found.");
				optMultiHP = true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
