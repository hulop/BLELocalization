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

import hulo.localization.sensor.SensorData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class Sample implements Cloneable{

	long timestamp;
	Location location;
	String uuid;
	List<Beacon> beacons;
	double[] beaconArray;

	List<SensorData> sensorDataList = null;
	private double invisibleMinRSSI2;
	public void setSensorDataList(List<SensorData> sensorDataList){
		this.sensorDataList = sensorDataList;
	}
	public List<SensorData> getSensorDataList(){
		return sensorDataList;
	}

	public Sample(Location location, String uuid, List<Beacon> beacons){
		setLocation(location);
		setUuid(uuid);
		setBeacons(beacons);
	}

	public Location getLocation() {
		return location;
	}
	public void setLocation(Location location) {
		this.location = location;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public List<Beacon> getBeacons() {
		return beacons;
	}
	public void setBeacons(List<Beacon> beacons) {
		this.beacons = beacons;
	}

	public void setTimeStamp(long timeStamp){
		this.timestamp = timeStamp;
	}
	public long getTimeStamp(){
		return timestamp;
	}

	public static List<Location> samplesToLocations(List<Sample> samples){
		int n = samples.size();
		List<Location> locations = new ArrayList<Location>();
		for(int i=0; i<n; i++){
			locations.add(samples.get(i).getLocation());
		}
		return locations;
	}

	public static List<List<Beacon>> samplesToBeaconsList(List<Sample> samples){
		int n = samples.size();
		List<List<Beacon>> beaconsList = new ArrayList<List<Beacon>>();
		for(int i=0; i<n; i++){
			beaconsList.add(samples.get(i).getBeacons());
		}
		return beaconsList;
	}

	public static List<List<Sample>> samplesToConsecutiveSamplesList(List<Sample> samples){

		int n= samples.size();
		List<List<Sample>> samplesList = new ArrayList<List<Sample>>();

		for(int i=0; i<n; i++){
			Sample smp = samples.get(i);
			Location loc = smp.getLocation();
			if(i==0){
				List<Sample> consecSamples = new ArrayList<Sample>();
				consecSamples.add(smp);
				samplesList.add(consecSamples);
			}else{
				List<Sample> lastConsecSamples = samplesList.get(samplesList.size()-1);
				Sample lastSmp = lastConsecSamples.get(lastConsecSamples.size()-1);
				Location lastLoc = lastSmp.getLocation();
				if(loc.equals(lastLoc)){
					lastConsecSamples.add(smp);
				}else{
					List<Sample> consecSamples = new ArrayList<Sample>();
					consecSamples.add(smp);
					samplesList.add(consecSamples);
				}
			}
		}
		//System.out.println("samplesList.size="+samplesList.size()+" in Sample.samplesToConsecutiveSamplesList");
		return samplesList;
	}


	public static List<Sample> samplesListToSamples(List<List<Sample>> samplesList){
		List<Sample> samplesNew = new ArrayList<Sample>();
		for(List<Sample> samples: samplesList){
			for(Sample sample: samples){
				samplesNew.add(sample);
			}
		}
		return samplesNew;
	}

	public static Sample mean(List<Sample> samples, double minRssi){
		return meanFillMissing(samples, minRssi);
	}

	public static Sample meanFillMissing(List<Sample> samples, double minRssi){

		String uuid = samples.get(0).getUuid();
		List<Location> locationList = new ArrayList<Location>();
		List<List<Beacon>> beaconsList = new ArrayList<List<Beacon>>();


		for(Sample sample: samples){
			locationList.add(sample.getLocation());
			beaconsList.add(sample.getBeacons());
		}

		Location locationAverage = Location.mean(locationList.toArray(new State[0]));
		List<Beacon> beaconsAverage = Beacon.meanFillMissing(beaconsList, minRssi);

		Sample meanSample = new Sample(locationAverage, uuid, beaconsAverage);
		meanSample.setTimeStamp(meanTimeStamp(samples));

		return meanSample;
	}


	public static long meanTimeStamp(List<Sample> samples){
		int n=samples.size();
		long timestamp_mean = 0;
		for(Sample sample: samples){
			timestamp_mean += sample.getTimeStamp();
		}
		timestamp_mean/=n;
		return timestamp_mean;
	}

	public static Sample meanTrim(List<Sample> samples, double minRssi){
		return meanTrimMinCount(samples, minRssi, 1);
	}

	public static Sample meanTrimMinCount(List<Sample> samples, double minRssi, int minCount){

		String uuid = samples.get(0).getUuid();
		List<Location> locationList = new ArrayList<Location>();
		List<List<Beacon>> beaconsList = new ArrayList<List<Beacon>>();

		for(Sample sample: samples){
			locationList.add(sample.getLocation());
			beaconsList.add(sample.getBeacons());
		}

		Location locationAverage = Location.mean(locationList.toArray(new State[0]));
		List<Beacon> beaconsAverage = Beacon.meanTrimMinCount(beaconsList, minCount);

		Sample meanSample = new Sample(locationAverage, uuid, beaconsAverage);
		meanSample.setTimeStamp(meanTimeStamp(samples));

		return meanSample;
	}


	public static List<Sample> meanList(List<List<Sample>> samplesList, double minRssi){
		return meanFillMissingList(samplesList, minRssi);
	}

	public static List<Sample> meanFillMissingList(List<List<Sample>> samplesList, double minRssi){
		List<Sample> samplesAvg = new ArrayList<Sample>();
		for(List<Sample> samples: samplesList){
			Sample meanSample = meanFillMissing(samples, minRssi);
			samplesAvg.add(meanSample);
		}
		return samplesAvg;
	}



	public static List<Sample> meanTrimList(List<List<Sample>> samplesList, double minRssi){
		return meanTrimMinCountList(samplesList, minRssi, 0);
	}

	public static List<Sample> meanTrimMinCountList(List<List<Sample>> samplesList, double minRssi, int minCount){
		List<Sample> samplesAvg = new ArrayList<Sample>();

		for(List<Sample> samples: samplesList){
			Sample meanSample = meanTrimMinCount(samples, minRssi, minCount);
			samplesAvg.add(meanSample);
		}
		return samplesAvg;
	}


	public static List<Sample> movingAverage(List<Sample> samples, int window){
		int n = samples.size();
		List<Sample> samplesNew = new ArrayList<Sample>();
		for(int i=0; i < n-window + 1; i++){
			List<Sample> smpsSub = samples.subList(i, i+window);
			Sample smp = meanTrim(smpsSub, Beacon.minRssi);
			samplesNew.add(smp);
		}
		return samplesNew;
	}

	public static List<List<Sample>> movingAverageList(List<List<Sample>> samplesList, int window){
		int nList = samplesList.size();
		List<List<Sample>> samplesListNew = new ArrayList<List<Sample>>();
		for(int i=0; i<nList; i++){
			List<Sample> smps = samplesList.get(i);
			samplesListNew.add(movingAverage(smps, window));
		}
		return samplesListNew;
	}

	public String toString() {
		String str = "";
		if (location != null) {
			str = location.toString();
		}
		str += ","+beacons.size()+"beacons";
		for(Beacon b: beacons) {
			str += ","+b.toString();
		}
		return str;
	}


	public static List<Location> samplesToAllLocations(List<Sample> samples){
		List<List<Sample>> samplesList = samplesToConsecutiveSamplesList(samples);
		List<Location> locs = new ArrayList<Location>();
		for(List<Sample> smps : samplesList){
			Sample s = smps.get(0);
			locs.add(s.getLocation());
		}
		return locs;
	}

	public static List<Location> generateRandomLocationsResample(List<Sample> samples, int n_particles){
		List<Location> locs = Sample.samplesToAllLocations(samples);
		int ns = locs.size();
		List<Location> randLocs = new ArrayList<Location>();
		Random rand = new Random();

		// For sampling without replacement
		List<Integer> intList = new ArrayList<Integer>();
		for(int i=0; i<ns; i++){ intList.add(i); }
		Collections.shuffle(intList);

		for(int i=0; i<n_particles; i++){
			int idx = rand.nextInt(ns);
			if(i<ns){
				idx = intList.get(i);
			}
			randLocs.add(locs.get(idx));
		}
		return randLocs;
	}

	private void checkArray() {
		if (this.beaconArray == null) {
			Set<Integer> allIds = Beacon.getAllIds(beacons);
			this.beaconArray = new double[allIds.size()];
			int j = 0;
			outer: for(Integer i: allIds) {
				for(Beacon b: beacons) {
					if (b.getId() == i) {
						this.beaconArray[j++] = b.getRssi();
						continue outer;
					}
				}
				//this.beaconArray[j++] = Beacon.minRssi;
				this.beaconArray[j++] = invisibleMinRSSI2;
			}
		}
	}

	public double distance(Sample s) {
		this.checkArray();
		s.checkArray();
		double d = 0;
		//double p = 0.5;
		for(int i = 0; i < beaconArray.length; i++) {
			//d += Math.pow(Math.abs(this.beaconArray[i] - s.beaconArray[i]),p);
			d += Math.abs(this.beaconArray[i] - s.beaconArray[i]);
		}
		//return Math.pow(d, 1/p);
		return d;
	}

	public double[] getBeaconArray(){
		this.checkArray();
		return this.beaconArray;
	}


	@Override
	public Sample clone(){
		try {
			return (Sample) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}


	public LocalizationInput toLocalizationInput(){
		LocalizationInput locInput = new LocalizationInput();
		locInput.setTimestamp(timestamp);
		locInput.setUuid(uuid);
		locInput.setBeacons(beacons);
		locInput.setSensorDataList(sensorDataList);
		return locInput;
	}

	public void adjust(HashMap<String, Integer> adjust) {
		for(Beacon b: beacons) {
			Integer rssi = adjust.get(b.getMajor()+"-"+b.getMinor());
			if (rssi != null) {
				b.setRssi((float)Math.max(b.getRssi()+rssi, Beacon.minRssi));
			}
		}
	}
	public boolean isInvalid() {
		for(Beacon b: beacons) {
			if (b.getRssi() > Beacon.minRssi) {
				return false;
			}
		}
		return true;
	}

	public void setInvisibleMinimum(double offset) {
		invisibleMinRSSI2 = Beacon.minRssi + offset;
	}

	public JSONObject toJSONObject(){
		JSONObject json = new JSONObject();

		try{
			JSONArray beaconsJArray = new JSONArray();
			JSONObject beaconsJObj = new JSONObject();
			beaconsJObj.put("uuid", uuid);
			JSONArray dataArray = beaconsToDataArray(beacons);
			beaconsJObj.put("data",dataArray);
			beaconsJObj.put("timestamp", timestamp);
			beaconsJArray.add(beaconsJObj);

			JSONObject informationJSON = new JSONObject();
			informationJSON.put("x", location.getX());
			informationJSON.put("y", location.getY());
			informationJSON.put("floor_num", location.getZ());
			informationJSON.put("floor", location.getZ());
			informationJSON.put("absx", location.getX());
			informationJSON.put("absy", location.getY());
			informationJSON.put("z", location.getH());

			json.put("beacons", beaconsJArray);
			json.put("information", informationJSON);

			JSONObject metadataJSON = new JSONObject();
			json.put("_metadata", metadataJSON);

		}catch(JSONException e){
			e.printStackTrace();
		}

		return json;
	}

	public String toCSVString(){
		StringBuilder sb = new StringBuilder();
		if(sensorDataList!=null){
			for(SensorData sen: sensorDataList){
				sb.append(sen.toCSVString());
				sb.append("\n");
			}
		}
		sb.append(timestamp);
		sb.append(",");
		sb.append("Beacon");
		sb.append(",");
		sb.append(location.getX());
		sb.append(",");
		sb.append(location.getY());
		sb.append(",");
		sb.append(location.getH());
		sb.append(",");
		sb.append(location.getZ());
		sb.append(",");

		int nBeacons = beacons.size();
		sb.append(nBeacons);
		for(Beacon b:beacons){
			sb.append(",");
			sb.append(b.getMajor());
			sb.append(",");
			sb.append(b.getMinor());
			sb.append(",");
			sb.append(b.getRssi());
		}
		return sb.toString();
	}

	public static String samplesToCSVString(List<Sample> samples){
		StringBuilder sb = new StringBuilder();
		for(Sample s: samples){
			sb.append(s.toCSVString());
			sb.append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}


	JSONArray beaconsToDataArray(List<Beacon> beacons){
		JSONArray dataArray = new JSONArray();
		for(Beacon b: beacons){
			JSONObject obj = b.toJSONObject();
			dataArray.add(obj);
		}
		return dataArray;
	}


	public static JSONArray samplesToJSONArray(List<Sample> samples){
		JSONArray jarray = new JSONArray();
		for(Sample s: samples){
			jarray.add(s.toJSONObject());
		}
		return jarray;
	}

}
