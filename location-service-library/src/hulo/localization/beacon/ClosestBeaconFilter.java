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
import hulo.localization.BLEBeacon;
import hulo.localization.State;
import hulo.localization.utils.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class ClosestBeaconFilter extends AbstractBeaconFilter implements BeaconFilter{

	static final String RANGE2D_KEY = "range2D";
	static final String RANGE_FLOOR_KEY = "rangeFloor";

	boolean isActive = true;

	double range2D= 10.0; //m
	double rangeFloor = 0.5; // floor

	List<BLEBeacon> bleBeacons;

	public ClosestBeaconFilter(){}

	@Override
	public void settingFromJSON(JSONObject params){
		try {
			double range2D = params.getDouble(RANGE2D_KEY);
			double rangeFloor = params.getDouble(RANGE_FLOOR_KEY);
			this.range2D = range2D;
			this.rangeFloor = rangeFloor;

			System.out.println(this.summary());

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public ClosestBeaconFilter setBLEBeacon(List<BLEBeacon> bleBeacons){
		this.bleBeacons = bleBeacons;
		return this;
	}

	@Override
	public List<Beacon> filter(List<Beacon> beacons, State[] states){
		if( isActive){
			int n = beacons.size();

			Set<Integer> closeMinorSet = new TreeSet<Integer>();
			for(BLEBeacon ib: bleBeacons){
				int minor = ib.getMinor();
				for(State st: states){
					double dist = st.getDistance(ib);
					double floorDiff = st.getFloorDifference(ib);
					if(dist<range2D && floorDiff<rangeFloor){
						closeMinorSet.add(minor);
						break;
					}
				}
			}

			List<Beacon> beaconsNew = new ArrayList<Beacon>();
			for(Beacon b: beacons){
				int minor = b.getMinor();
				if(closeMinorSet.contains(minor)){
					beaconsNew.add(b);
				}
			}

			int m = beaconsNew.size();

			if(n!=m){
				System.out.println("closest beacon filter reduced no. beacons "+n+">>"+m);
			}

			return beaconsNew;
		}else{
			return beacons;
		}
	}

	public String summary(){
		return ClassUtils.summary(this);
	}

}
