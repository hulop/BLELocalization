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

package hulo.localization.utils;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;

public class JSONUtils {

	static public JSONArray toJSONArray(double[] x){
		assert x!=null;
		int n=x.length;

		JSONArray jarray = new JSONArray();
		for(int i=0; i<n; i++){
			jarray.add(x[i]);
		}
		return jarray;
	}

	static public JSONArray toJSONArray(double[][] X){
		assert X!=null;
		int n=X.length;
		JSONArray jarray = new JSONArray();
		for(int i=0; i<n; i++){
			jarray.add(toJSONArray(X[i]));
		}
		return jarray;
	}

	static public double[] array1DfromJSONArray(JSONArray jarray){
		int n=jarray.size();
		double[] vec = new double[n];
		for(int i=0; i<n; i++){
			try {
				vec[i] = jarray.getDouble(i);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return vec;
	}

	static public double[][] array2DFromJSONArray(JSONArray jarray){
		int n = jarray.size();
		double[][] array = null;
		try{
			double[] vec = array1DfromJSONArray(jarray.getJSONArray(0));
			int m= vec.length;
			array = new double[n][m];
			for(int i=0; i<n; i++){
				array[i] = array1DfromJSONArray(jarray.getJSONArray(i));
			}
		}catch (JSONException e) {
			e.printStackTrace();
		}
		return array;
	}


}
