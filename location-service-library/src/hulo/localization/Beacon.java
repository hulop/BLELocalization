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

package hulo.localization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

/**
 * Data type to store beacon identifier and RSSI
 */
public class Beacon {
	static public double unObsRssi=0.0;
	static public double maxRssi = -1;
	static public double minRssi = -100;
	static private Map<String, Set<Integer>> allIds = new TreeMap<String, Set<Integer>>();

	static public void setMinRssi(double value){
		minRssi = value;
	}
	static public void setMaxRssi(double value){
		maxRssi = value;
	}

	private int id;
    private int major;
    private int minor;
    private float rssi;
    private String group;

    public Beacon(){}

    public Beacon(int major, int minor, float rssi) {
    	this(major, minor, rssi, "default");
    }

    public Beacon(int major, int minor, float rssi, String group){
        setGroup(group);
        setMajor(major);
        setMinor(minor);
        setRssi(rssi);
    }

    public void setMajor(int major){ this.major = major; }
    public void setMinor(int minor){
    	this.minor = minor;
    	this.id = minor;
    	getAllIds(this.group).add(this.id);
    }
    public void setRssi(float rssi){ this.rssi = rssi; }
    public void setGroup(String group) {this.group = group;}
    public int getMajor(){ return this.major; }
    public int getMinor(){ return this.minor; }
    public float getRssi(){ return this.rssi; }
    public String getGroup() {return this.group;}

    public static Set<Integer> getAllIds(List<?> list) {
    	return getAllIds(findGroup(list));
    }

    public static Set<Integer> getAllIds(String group) {
    	group = group==null?"default":group;
    	if (!allIds.containsKey(group)) {
    		Set<Integer> allId = new TreeSet<Integer>();
    		allIds.put(group, allId);
    	}
    	return allIds.get(group);
    }

    public void setId(int id){ this.id=id; }
    public int getId(){ return id; }


    public String toString(){
    	return id+","+major+","+minor+","+rssi;
    }

    public void print(){
        System.out.println("Beacon:(id,major,minor,rssi)=("+toString()+")");
    }

    public int compare(Beacon o1, Beacon o2){
    	return Integer.valueOf(o1.getId()).compareTo(o2.getId());
    }


    public static List<Beacon> fillUnObsRssi(List<Beacon> beacons){
    	List<Beacon> beaconsNew = new ArrayList<Beacon>();
		Iterator<Beacon> iter = beacons.iterator();
		while(iter.hasNext()){
			Beacon beacon = iter.next();
			float rssi = beacon.getRssi();
			if(minRssi < rssi && rssi < maxRssi){
				beaconsNew.add(beacon);
			}else{
				beacon.setRssi((float) minRssi);
				beaconsNew.add(beacon);
			}
		}
		return beaconsNew;
    }

    public static List<Beacon> filterBeacons(List<Beacon> beacons){
    	return filterBeacons(beacons, Beacon.minRssi, Beacon.maxRssi);
    }

    public static List<Beacon> filterBeacons(List<Beacon> beacons, double minRssi, double maxRssi){
		List<Beacon> beaconsNew = new ArrayList<Beacon>();
		Iterator<Beacon> iter = beacons.iterator();
		while(iter.hasNext()){
			Beacon beacon = iter.next();
			float rssi = beacon.getRssi();
			if(minRssi < rssi && rssi < maxRssi){
				beaconsNew.add(beacon);
			}
		}
		return beaconsNew;
	}

    public static List<Beacon> filterBeaconsTop(List<Beacon> beacons, int n){
    	if(beacons.size()<=n){
    		return beacons;
    	}
    	List<Beacon> beaconsSub;
    	beaconsSub = sortBeaconsByRssi(beacons);
    	return beaconsSub.subList(0, n);
    }

    public static List<Beacon> sortBeaconsByRssi(List<Beacon> beacons){
    	Collections.sort(beacons, new Comparator<Beacon>(){
    		@Override
    		public int compare(Beacon o1, Beacon o2){
    			final int DEC = -1;
    			float rssi1 = o1.getRssi();
    			float rssi2 = o2.getRssi();
    			return DEC*Float.compare(rssi1, rssi2);
    		}
    	});
    	return beacons;
    }

    public static List<Beacon> mean(List<List<Beacon>> beaconsList, double minRssi){
    	return meanFillMissing(beaconsList, minRssi);
    }

    public static List<Beacon> meanFillMissing(List<List<Beacon>> beaconsList, double minRssi){

    	int n_samples = beaconsList.size();
    	String group = findGroup(beaconsList);

    	Set<Integer> beaconIdSet= new HashSet<Integer>();
    	Map<Integer, Integer> majorMap = new HashMap<Integer, Integer>();
    	Map<Integer, Integer> minorMap = new HashMap<Integer, Integer>();
    	Map<Integer, Double> sumOfRssiMap = new HashMap<Integer, Double>();
    	Map<Integer, Integer> numOfRssiMap = new HashMap<Integer, Integer>();

    	for(List<Beacon> beacons: beaconsList){
    		for(Beacon beacon: beacons){
    			int id = beacon.getId();
    			int major = beacon.getMajor();
    			int minor = beacon.getMinor();
    			double rssi = beacon.getRssi();

    			Double sumRssi = sumOfRssiMap.get(id);
    			Integer numOfRssi = numOfRssiMap.get(id);
    			if(sumRssi==null){
    				sumRssi = rssi;
    				beaconIdSet.add(id);
       				majorMap.put(id, major);
    				minorMap.put(id,minor);
    				numOfRssi=1;
    			}else{
    				sumRssi += rssi;
    				numOfRssi++;
    			}
    			sumOfRssiMap.put(id, sumRssi);
    			numOfRssiMap.put(id, numOfRssi);
    		}
    	}

    	List<Beacon> meanBeacons = new ArrayList<Beacon>();
    	for(Integer id: beaconIdSet){
    		Integer major = majorMap.get(id);
    		Integer minor = minorMap.get(id);
    		Double sumRssi = sumOfRssiMap.get(id);
			Integer numOfRssi = numOfRssiMap.get(id);
			for(int i=numOfRssi; i<n_samples; i++){
				sumRssi += minRssi;
			}
			double meanOfRssi = sumRssi/n_samples;
			Beacon meanBeacon = new Beacon(major, minor, (float) meanOfRssi, group);
			meanBeacon.setId(id);
			meanBeacons.add(meanBeacon);
    	}

    	return meanBeacons;
    }

    public static List<Beacon> meanTrim(List<List<Beacon>> beaconsList){
    	return meanTrimMinCount(beaconsList, 0);
    }

    public static List<Beacon> meanTrimMinCount(List<List<Beacon>> beaconsList, int minCount){
    	String group = findGroup(beaconsList);
    	Set<Integer> beaconIdSet= new HashSet<Integer>();
    	Map<Integer, Integer> majorMap = new HashMap<Integer, Integer>();
    	Map<Integer, Integer> minorMap = new HashMap<Integer, Integer>();
    	Map<Integer, Double> sumOfRssiMap = new HashMap<Integer, Double>();
    	Map<Integer, Integer> numOfRssiMap = new HashMap<Integer, Integer>();

    	for(List<Beacon> beacons: beaconsList){
    		for(Beacon beacon: beacons){
    			int id = beacon.getId();
    			int major = beacon.getMajor();
    			int minor = beacon.getMinor();
    			double rssi = beacon.getRssi();

    			Double sumRssi = sumOfRssiMap.get(id);
    			Integer numOfRssi = numOfRssiMap.get(id);
    			if(sumRssi==null){
    				sumRssi = rssi;
    				beaconIdSet.add(id);
    				majorMap.put(id, major);
    				minorMap.put(id,minor);
    				numOfRssi=1;
    			}else{
    				sumRssi += rssi;
    				numOfRssi++;
    			}
    			sumOfRssiMap.put(id, sumRssi);
    			numOfRssiMap.put(id, numOfRssi);
    		}
    	}

    	List<Beacon> meanBeacons = new ArrayList<Beacon>();
    	for(Integer id: beaconIdSet){
    		Integer major = majorMap.get(id);
    		Integer minor = minorMap.get(id);
			Integer numOfRssi = numOfRssiMap.get(id);
			if(minCount <= numOfRssi){
				Double sumRssi = sumOfRssiMap.get(id);
				double meanOfRssi = sumRssi/numOfRssi;
				Beacon meanBeacon = new Beacon(major, minor, (float) meanOfRssi, group);
				meanBeacon.setId(id);
				meanBeacons.add(meanBeacon);
			}
    	}
    	return meanBeacons;
    }


    public static double[] beaconsToArray(List<Beacon> beacons){
    	String group = "";
    	if ( beacons.size() > 0) {
    		group = beacons.get(0).getGroup();
    	}

    	double[] beaconArray = new double[Beacon.getAllIds(group).size()];
		int j = 0;

		outer: for(Integer i: Beacon.getAllIds(group)) {
			for(Beacon b: beacons) {
				if (b.getId() == i) {
					beaconArray[j++] = b.getRssi();
					continue outer;
				}
			}
			beaconArray[j++] = Beacon.minRssi;
		}
		return beaconArray;
	}

    public static double[][] beaconsListToArray(List<List<Beacon>> beaconsList) {
    	int n=beaconsList.size();
    	String group = findGroup(beaconsList.get(0));

    	double[][] beaconsArray = new double[n][Beacon.getAllIds(group).size()];
    	for(int i=0; i<n; i++){
    		List<Beacon> beacons = beaconsList.get(i);
    		beaconsArray[i] = beaconsToArray(beacons);
    	}
    	return beaconsArray;
    }

    public static String findGroup(List<?> list) {
    	String group = null;
    	for(int i = 0; i < list.size(); i++) {
    		if (list.get(i) == null) {
    			continue;
    		}
    		if (list.get(i) instanceof List<?>) {
    			group = findGroup((List<?>)list.get(i));
    		}
    		if (list.get(i) instanceof Sample) {
    			group = findGroup(((Sample)list.get(i)).beacons);
    		}
    		if (list.get(i) instanceof Beacon) {
    			group = ((Beacon)list.get(i)).getGroup();
    		}
    		if (group != null) {
    			return group;
    		}
    	}
    	return null;
    }

    public JSONObject toJSONObject(){
    	JSONObject json = new JSONObject();
    	try{
    		json.put("major", this.getMajor());
    		json.put("minor", this.getMinor());
    		json.put("rssi", this.getRssi());
    	} catch(JSONException e){
    		e.printStackTrace();
    	}
    	return json;
    }
}