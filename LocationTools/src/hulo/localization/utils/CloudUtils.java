/*******************************************************************************
 * Copyright (c) 2014, 2023  IBM Corporation, Carnegie Mellon University and others
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

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONObject;

public class CloudUtils {

	public static String getCredentialURL(String[] keys, String defaultURL) {
		return getCredential(keys, "url", defaultURL);
	}

	public static String getCredential(String[] keys, String key2, String defaultValue) {
		try {
			String vcap = System.getenv("HULOP_VCAP_SERVICES");
			if (vcap == null) {
				vcap = System.getenv("VCAP_SERVICES");
			}
			if (vcap != null) {
				JSONObject json = (JSONObject) JSON.parse(vcap);
				for (String key : keys) {
					if (json.has(key)) {
						System.out.println(json.toString(4));
						return json.getJSONArray(key).getJSONObject(0).getJSONObject("credentials").getString(key2);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultValue;
	}

	public static JSONObject getCredential(String[] keys) {
		try {
			String vcap = System.getenv("HULOP_VCAP_SERVICES");
			if (vcap == null) {
				vcap = System.getenv("VCAP_SERVICES");
			}
			if (vcap != null) {
				JSONObject json = (JSONObject) JSON.parse(vcap);
				for (String key : keys) {
					if (json.has(key)) {
						System.out.println(json.toString(4));
						return json.getJSONArray(key).getJSONObject(0).getJSONObject("credentials");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
