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

import hulo.localization.Beacon;
import hulo.localization.State;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class NormedStrongestBeaconFilter extends AbstractBeaconFilter implements BeaconFilter{

	static final String K_BEACONS_KEY = "kBeacons";
	static final String DO_NORMALIZE_KEY = "doNormalize";

	int kBeacons = 10;
	boolean doNormalize = true;
	BLEBeaconConfig ibconfig = null;

	public NormedStrongestBeaconFilter(){}

	@Override
	public void settingFromJSON(JSONObject json){
		int k;
		try {
			k = json.getInt(K_BEACONS_KEY);
			doNormalize = json.getBoolean(DO_NORMALIZE_KEY);
			kBeacons = k;
			System.out.println(this.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected BLEBeaconConfig getBLEBeaconConfig(){
		if(ibconfig == null){
			ibconfig = BLEBeaconConfig.createFrom(bleBeacons);
		}
		return ibconfig;
	}

	@Override
	public List<Beacon> filter(List<Beacon> beacons, State[] states){

		int n = beacons.size();
		if(n < kBeacons){
			return beacons;
		}

		final boolean doNormalize = this.doNormalize;
		final BLEBeaconConfig ibconfig = getBLEBeaconConfig();

		Collections.sort(beacons, new Comparator<Beacon>(){
			int DEC = -1;
			@Override
			public int compare(Beacon b1, Beacon b2) {
				int major1 = b1.getMajor();
				int minor1 = b1.getMinor();
				double rssi1 = b1.getRssi();

				int major2 = b2.getMajor();
				int minor2 = b2.getMinor();
				double rssi2 = b2.getRssi();

				double msdPower1 = 0;
				double msdPower2 = 0;
				if(doNormalize){
					msdPower1 = ibconfig.getMsdPower(major1, minor1);
					msdPower2 = ibconfig.getMsdPower(major2, minor2);
				}

				double normedRssi1 = rssi1 - msdPower1;
				double normedRssi2 = rssi2 - msdPower2;

				if(normedRssi1 > normedRssi2){
					return 1*DEC;
				}else if(normedRssi1 == normedRssi2){
					return 0;
				}else{
					return -1*DEC;
				}
			}
		});
		int m = kBeacons < n ? kBeacons : n;

		List<Beacon> beaconsTmp = beacons.subList(0, m);

		final StringBuilder sb = new StringBuilder();
		if (n != m) {
			sb.setLength(0);
			sb.append("NormedStrongestBeaconFilter reduced the number of beacons "+n+">>"+m+". ");
			sb.append("RSSI: ");
			for(Beacon b: beaconsTmp){
				sb.append(b.getRssi()+",");
			}
			System.out.println(sb.toString());
		}
		return beaconsTmp;
	}

	public String toString() {
		return "NormedStrongestBeaconFilter (k=" + kBeacons + ", doNormalize="+ doNormalize + ")";
	}
}


