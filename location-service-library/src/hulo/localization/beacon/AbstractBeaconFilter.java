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

import hulo.localization.BLEBeacon;
import hulo.localization.Beacon;
import hulo.localization.State;

import java.util.List;

import org.apache.wink.json4j.JSONObject;

public class AbstractBeaconFilter implements BeaconFilter{

	List<BLEBeacon> bleBeacons;
	public AbstractBeaconFilter(){}

	public void settingFromJSON(JSONObject json){
		// pass
	}

	public BeaconFilter setBLEBeacon(List<BLEBeacon> bleBeacons){
		this.bleBeacons = bleBeacons;
		return this;
	}
	public List<Beacon> filter(List<Beacon> beacons, State[] states){
		return beacons;
	}
}
