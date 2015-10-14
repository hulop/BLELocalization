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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BLEBeaconConfig {

	Set<List<Integer>> set = new HashSet<List<Integer>>();
	Map<List<Integer>, Integer> outputPowerMap = new HashMap<List<Integer>, Integer>();
	Map<List<Integer>, Integer> msdPowerMap = new HashMap<List<Integer>, Integer>();

	public Set<List<Integer>> keySet(){
		return set;
	}

	public void putOutputPower(Integer major, Integer minor, Integer value){
		List<Integer> list = new ArrayList<Integer>();
		list.add(major);
		list.add(minor);
		set.add(list);
		outputPowerMap.put(list, value);
	}

	public void putMsdPower(Integer major, Integer minor, Integer value){
		List<Integer> list = new ArrayList<Integer>();
		list.add(major);
		list.add(minor);
		set.add(list);
		msdPowerMap.put(list, value);
	}

	public Integer getOutputPower(Integer major, Integer minor){
		List<Integer> list = new ArrayList<Integer>();
		list.add(major);
		list.add(minor);
		Integer outPower = outputPowerMap.get(list);
		return outPower;
	}

	public Integer getMsdPower(Integer major, Integer minor){
		List<Integer> list = new ArrayList<Integer>();
		list.add(major);
		list.add(minor);
		Integer msdPower = msdPowerMap.get(list);
		return msdPower;
	}

	public List<BLEBeacon> assignPropertiesToBLEBeacons(List<BLEBeacon> bleBeacons){
		for(BLEBeacon ib : bleBeacons){
			int major = ib.getMajor();
			int minor = ib.getMinor();
			Integer outputPower = getOutputPower(major, minor);
			Integer msdPower = getMsdPower(major, minor);
			if(outputPower==null){
				System.out.println("Beacon(major"+major+",minor="+minor+") is not registered in beacon.csv file.");
				continue;
			}
			ib.setOutputPower(outputPower);
			ib.setMsdPower(msdPower);
		}
		return bleBeacons;
	}

	public static BLEBeaconConfig createFrom(List<BLEBeacon> bleBeacons){
		BLEBeaconConfig ibconfig = new BLEBeaconConfig();
		for(BLEBeacon ib : bleBeacons){
			int major = ib.getMajor();
			int minor = ib.getMinor();
			int outputPower = ib.getOutputPower();
			int msdPower = ib.getMsdPower();
			ibconfig.putOutputPower(major, minor, outputPower);
			ibconfig.putMsdPower(major, minor, msdPower);
		}
		return ibconfig;
	}
}
