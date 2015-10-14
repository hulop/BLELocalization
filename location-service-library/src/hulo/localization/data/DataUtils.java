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

package hulo.localization.data;

import hulo.localization.BLEBeacon;
import hulo.localization.Beacon;
import hulo.localization.Location;
import hulo.localization.Sample;
import hulo.localization.beacon.BLEBeaconConfig;
import hulo.localization.sensor.SensorData;
import hulo.localization.utils.NumberUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class DataUtils {
	// Default parameter
	public static final int DEFAULT_STEP = 1;
	public static final String strCode = "Shift_JIS";

	static final int major_default = 100;
	static int major = major_default;

	public static void setMajor(int major){
		DataUtils.major = major;
	}

	public static int getMajor(){
		return DataUtils.major;
	}

	// Sampling data read method
	static final String READ_JSON_SAMPLES = "readJSONSamples";
	static final String READ_CSV_WALKING_SAMPLES = "readCSVWalkingSamples";

	// BLEBeacon location read method
	static final String READ_JSON_BLEBEACON_LOCATION = "readJSONBLEBeaconLocation";

	private DataUtils(){
		throw new AssertionError();
	}

	public static InputStreamReader newInputStreamReader(InputStream is){
		try {
			return new InputStreamReader(is, strCode);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected static OutputStreamWriter newOutputStreamWriter(OutputStream os){
		try {
			return new OutputStreamWriter(os, strCode);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}


	public static Location[] readLocations(InputStream is) {
		/*
		 * This method assumes the csv format as follows:
		 *
		 * x0,y0,z0
		 * x1,y1,z1
		 * ...
		 * xn,yn,zn
		 */
		List<Location> locations = new ArrayList<Location>();
		try {
			InputStreamReader isr = newInputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null) {
				String[] strings = line.split(",");
				float x = Float.parseFloat(strings[0]);
				float y = Float.parseFloat(strings[1]);
				float z = Float.parseFloat(strings[2]);
				locations.add(new Location(x, y, z));
			}
			br.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return locations.toArray(new Location[locations.size()]);
	}

	public static List<BLEBeacon> readBLEBeaconLocation(InputStream is) {
		final int startData = 1;
		final int atId = 0;
		final int atMajor = 1;
		final int atMinor = 2;
		final int atX = 3;
		final int atY = 4;
		final int atFloor = 5;

		List<BLEBeacon> bleBeacons = new ArrayList<BLEBeacon>();
		try {
			InputStreamReader isr = newInputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			float x;
			float y;
			float z;
			int count = 0;
			while ((line = br.readLine()) != null) {
				if(startData<=count){
					String[] strings = line.split(",");
					int id = Integer.parseInt(strings[atId]);
					int major = Integer.parseInt(strings[atMajor]);
					int minor = Integer.parseInt(strings[atMinor]);
					x = Float.parseFloat(strings[atX]);
					y = Float.parseFloat(strings[atY]);
					z = Float.parseFloat(strings[atFloor]);
					BLEBeacon BLEBeacon = new BLEBeacon(id,major,minor,x,y,z);
					bleBeacons.add(BLEBeacon);
				}
				count++;
			}
			br.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return bleBeacons;
	}

	public static List<BLEBeacon> readJSONBLEBeaconLocation(InputStream is){
		try{
			List<BLEBeacon> bleBeacons = new ArrayList<BLEBeacon>();

			JSONArtifact json = JSON.parse(is);
			if(json instanceof JSONArray){
				JSONArray array = (JSONArray) json;

				for( int i=0; i<array.size(); i++){
					JSONObject obj = array.getJSONObject(i);
					BLEBeacon ib = JSONObjectToBLEBeacon(obj);
					bleBeacons.add(ib);
				}
			}
			return bleBeacons;
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static JSONArray BLEBeaconsToJSONArray(List<BLEBeacon> bleBeacons){
		JSONArray jarray = new JSONArray();
		for(BLEBeacon ib :bleBeacons){
			JSONObject obj = BLEBeaconToJSONObject(ib);
			jarray.add(obj);
		}
		return jarray;
	}

	public static BLEBeacon JSONObjectToBLEBeacon(JSONObject obj){
		BLEBeacon BLEBeacon = null;
		try{
			double x = obj.getDouble("x");
			double y = obj.getDouble("y");
			double z  = floorStringToFloorNum(obj.getString("f"));
			int id = obj.getInt("id");
			int major = DataUtils.major;
			int minor = id;

			if(obj.containsKey("major")){
				major = obj.getInt("major");
			}
			if(obj.containsKey("minor")){
				minor = obj.getInt("minor");
			}
			BLEBeacon = new BLEBeacon(id, major, minor, x, y, z);

		}catch(JSONException e){
			e.printStackTrace();
		}
		return BLEBeacon;
	}

	public static JSONObject BLEBeaconToJSONObject(BLEBeacon BLEBeacon){

		JSONObject jobj = new JSONObject();
		try{
			jobj.put("type","beacon");

			jobj.put("x", BLEBeacon.getX());
			jobj.put("y", BLEBeacon.getY());
			jobj.put("f", BLEBeacon.getZ());

			jobj.put("id", BLEBeacon.getMinor());
			jobj.put("major", BLEBeacon.getMajor());
			jobj.put("minor", BLEBeacon.getMinor());
			jobj.put("msdPower", BLEBeacon.getMsdPower());
			jobj.put("outPower", BLEBeacon.getOutputPower());
		}catch(JSONException e){
			e.printStackTrace();
		}
		return jobj;
	}

	public static int floorStringToFloorNum(String floor){
		String floorTrimed = floor.trim();
		if(floorTrimed.equals("")){
			System.out.println("Floor string is empty. Set to 0.");
			return 0;
		}
		if(NumberUtils.isInt(floorTrimed)){
			return Integer.parseInt(floorTrimed);
		}
		char chead = floorTrimed.charAt(0);
		int iend = floorTrimed.length();
		char cend = floorTrimed.charAt(iend-1);
		if(cend!='F'){
			if(NumberUtils.isNumber(floorTrimed)){
				return (int) Double.parseDouble(floorTrimed);
			}else{
				System.out.println("unknown floor ["+floorTrimed+"] is detected. Set to "+"0.");
				return 0;
			}
		}
		int floor_num;
		if(chead=='B'){
			String num = floorTrimed.substring(1, iend-1);
			floor_num = -1*Integer.parseInt(num);
		}else{
			String num = floorTrimed.substring(0, iend-1);
			floor_num = Integer.parseInt(num) - 1;
		}
		return floor_num;
	}

	public static boolean hasReadMethod(String readMethod){
		if(readMethod.equals(READ_JSON_SAMPLES)){
			return true;
		}else if(readMethod.equals(READ_CSV_WALKING_SAMPLES)){
			return true;
		}else{
			return false;
		}
	}

	public static List<Sample> readSamplesInterface(InputStream is, String readMethod, String group){
		List<Sample> samples = null;
		if(readMethod.equals(READ_JSON_SAMPLES)){
			samples = readJSONSamples(is, group);
		}else if(readMethod.equals(READ_CSV_WALKING_SAMPLES)){
			samples = readCSVWalkingSamples(is, group);
		}else{
			System.out.println("unknown read method");
			try {
				throw new IOException();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return samples;
	}

	public static List<Sample> readJSONSamples(InputStream is) {
		return readJSONSamples(is, null, false);
	}

	public static List<Sample> readJSONSamples(InputStream is, boolean removeMinRssi) {
		return readJSONSamples(is, null, removeMinRssi);
	}

	public static List<Sample> readJSONSamples(InputStream is, String group) {
		return readJSONSamples(is, group, false);
	}

	public static List<Sample> readJSONSamples(InputStream is, String group, boolean removeMinRssi) {
		try {
			List<Sample> samples = new ArrayList<Sample>();

			String uuid = "";
			JSONArtifact json = JSON.parse(is);
			if (json instanceof JSONArray) {
				JSONArray array = (JSONArray)json;

				for(int i = 0; i < array.size(); i++) {
					try {
						JSONObject obj = array.getJSONObject(i);
						JSONArray beacons = obj.getJSONArray("beacons");
						JSONObject info = obj.getJSONObject("information");

						Location loc = new Location();

						String xKey = "absx";
						String yKey = "absy";
						String zKey = "z";
						String floorKey = "floor";
						String floorNumKey = "floor_num";
						loc.setX((float)info.getDouble(xKey));
						loc.setY((float)info.getDouble(yKey));

						loc.setH((float)info.getDouble(zKey));

						String floor = info.getString(floorKey);
						loc.setFloor(floor);
						loc.setZ((float) info.getDouble(floorNumKey));

						// Check if the sampling data is invalidated, and skip the invalidated sampling data
						JSONObject metadata = obj.getJSONObject("_metadata");
						if(metadata.containsKey("comment")){
							String comment = metadata.getString("comment");
							if(comment.contains("invalidated")){
								String createdAt = metadata.getJSONObject("created_at").getString("$date");
								System.out.println("sampling data at location=("
										+loc.toString()+ ") created at "
										+createdAt+" is invalidated.");
								continue;
							}
						}

						for(int j = 0; j < beacons.size(); j++) {
							JSONObject bs = beacons.getJSONObject(j);
							// Beacon data must have a "data" key.
							if(bs.containsKey("data")){
								long timestamp = bs.getLong("timestamp");
								JSONArray data = bs.getJSONArray("data");
								List<Beacon> bsList = new ArrayList<Beacon>();
								for(int k = 0; k < data.size(); k++) {
									JSONObject item = data.getJSONObject(k);
									int rssi = item.getInt("rssi");
									if(rssi == Beacon.unObsRssi || rssi < Beacon.minRssi){
										rssi = (int) Beacon.minRssi;
									}
									Beacon beacon = new Beacon(item.getInt("major"), item.getInt("minor"), rssi, group);
									if(removeMinRssi){
										if(rssi > Beacon.minRssi){
											bsList.add(beacon);
										}
									}else{
										bsList.add(beacon);
										if(beacon.getRssi() < Beacon.minRssi){
											System.out.println("rssi="+beacon.getRssi());
										}
									}
								}
								Sample smp = new Sample(loc,uuid,bsList);
								smp.setTimeStamp(timestamp);
								samples.add(smp);
							}else if(bs.containsKey("type")){
								String type = bs.getString("type");
								//System.out.println("A "+type+" type JSON object was neglected");
							}
							else{
								System.out.println("A JSON object in a beacons array does not contain key:[data]. Neglected.");
							}
						}
					}catch(Exception e) {
						System.out.println(e.getMessage());
					}
				}
			}
			return samples;

		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<Sample> readCSVWalkingSamples(InputStream is){
		return readCSVWalkingSamples(is, null);
	}
	public static List<Sample> readCSVWalkingSamples(InputStream is, String group){
		return readCSVWalkingSamples(is, group, false);
	}

	public static List<Sample> readCSVWalkingSamples(InputStream is, String group, boolean removeMinRssi){
		List<Sample> samples = new ArrayList<Sample>();
		InputStreamReader in;
		try {
			in = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(in);
			String line;
			List<SensorData> sensorDataList = new ArrayList<SensorData>();
			while((line=br.readLine()) != null){
				SensorData sens = parseSensorData(line);
				Sample smp = parseBeaconsData(line, "Beacon", group, removeMinRssi);
				if(sens!=null){
					sensorDataList.add(sens);
				}
				if(smp!=null){
					smp.setSensorDataList(sensorDataList);
					samples.add(smp);
					sensorDataList = new ArrayList<SensorData>();
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return samples;
	}

	private static double tokenToDouble(String token){
		if(token.equals("")){
			System.out.println("location is empty. Set to 0.");
			return 0;
		}else{
			return Double.parseDouble(token);
		}
	}

	static final String beaconHet = "Beacon";
	public static Sample parseBeaconsData(String line, String dataType, boolean removeMinRssi){
		return parseBeaconsData(line, dataType, null, removeMinRssi);
	}
	public static Sample parseBeaconsData(String line, String dataType, String group, boolean removeMinRssi){
		String uuid = "";
		Sample smp = null;

		String[] tokens = line.split(", ?");
		for(int i=0; i<tokens.length; i++){
			tokens[i] = tokens[i].trim();
		}
		long timestamp = Double.valueOf(tokens[0].trim()).longValue();
		String type = tokens[1].trim();
		if(dataType.equals(beaconHet)){
			if(type.equals(beaconHet) ){
				Location loc = null;
				if(tokens[2].equals("") || tokens[3].equals("") || tokens[4].equals("")){
					loc = null;
				}else{
					double x = tokenToDouble(tokens[2]); //Double.parseDouble(tokens[2]);
					double y = tokenToDouble(tokens[3]); //Double.parseDouble(tokens[3]);
					double z = tokenToDouble(tokens[4]); //Double.parseDouble(tokens[4]); // Not used
					String floor = tokens[5];
					double floorNum = floorStringToFloorNum(floor);
					loc = new Location(x, y, floorNum);
					loc.setFloor(floor);
					loc.setH((float)z);
				}
				List<Beacon> beacons = new ArrayList<Beacon>();
				int count = Integer.parseInt(tokens[6]);
				for(int i=0; i<count; i++){
					int major = Integer.parseInt(tokens[7+ 3*i + 0]);
					int minor = Integer.parseInt(tokens[7+ 3*i + 1]);
					double rssi = Double.parseDouble(tokens[7+ 3*i + 2]);
					if(rssi == Beacon.unObsRssi || rssi < Beacon.minRssi){
						rssi = Beacon.minRssi;
					}
					Beacon b = new Beacon(major, minor, (float) rssi, group);
					if(removeMinRssi){
						if(rssi > Beacon.minRssi){
							beacons.add(b);
						}else{
							// do not add a new beacon
						}
					}else{
						beacons.add(b);
					}
				}
				smp = new Sample(loc, uuid, beacons);
				smp.setTimeStamp(timestamp);
			}
		}
		return smp;
	}


	public static SensorData parseSensorData(String line){
		assert line!=null;
		if(line=="" || line==null){
			return null;
		}

		String[] tokens = line.split(", ?");
		int n = tokens.length;
		for(int i=0; i<n; i++){
			tokens[i] = tokens[i].trim();
		}

		long timestamp = Double.valueOf(tokens[0].trim()).longValue();

		String type = tokens[1].trim();
		if(! type.equals(beaconHet)){
			try{
				SensorData sensorData = new SensorData();
				sensorData.setTimestamp(timestamp);
				sensorData.setType(type);
				double[] data = new double[n-2];
				for(int j=2; j<n; j++){
					if(NumberUtils.isNumber(tokens[j])){
						data[j-2] = Double.parseDouble(tokens[j]);
					}else{
						return null;
					}
				}
				sensorData.setData(data);
				return sensorData;
			}catch(NumberFormatException e){
				e.printStackTrace();
				return null;
			}
		}else{
			return null;
		}
	}


	public static List<Sample> readSamples(InputStream is) {
		return readSamples(is);
	}

	public static List<Sample> readSamples(InputStream is, String group) {
		List<Sample> samples = new ArrayList<Sample>();
		try {
			InputStreamReader reader = newInputStreamReader(is);
			BufferedReader br = new BufferedReader(reader);
			String line;
			String uuid = "";
			int i = 0;
			while (true) {
				line = br.readLine();
				if(line == null) {
					break;
				}
				if (i >= 1) {
					String[] strings = line.split(",");
					float x = Float.parseFloat(strings[1]);
					float y = Float.parseFloat(strings[2]);
					float z = Float.parseFloat(strings[3]);
					Location location = new Location(x, y, z);
					int count = Integer.parseInt(strings[4]);
					List<Beacon> beacons = new ArrayList<Beacon>();
					for (int j = 0; j < count; j++) {
						String[] beaconsStrings = strings[5 + j].split(":");
						int major = Integer.parseInt(beaconsStrings[0]);
						int minor = Integer.parseInt(beaconsStrings[1]);
						float rssi = Float.parseFloat(beaconsStrings[2]);
						Beacon beacon = new Beacon(major, minor, rssi, group);
						beacons.add(beacon);
					}
					samples.add(new Sample(location, uuid, beacons));
				}
				i++;
			}
			br.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return samples;
	}

	public static List<List<Sample>> readSamplesList(InputStream[] iss) {
		return readSamplesList(iss, DEFAULT_STEP, null);
	}

	public static List<List<Sample>> readSamplesList(InputStream[] iss, String group) {
		return readSamplesList(iss, DEFAULT_STEP, group);
	}

	public static List<List<Sample>> readSamplesList(InputStream[] iss, int step, String group) {
		List<List<Sample>> samplesList = new ArrayList<List<Sample>>();
		int count = 0;
		for (InputStream is : iss) {
			if (count % step == 0) {
				List<Sample> samples = readSamples(is, group);
				if(samples.size()==0){
					System.out.println("samples.size()==0");
					RuntimeException e =  new RuntimeException();
					e.printStackTrace();
					throw e;
				}
				samplesList.add(samples);
			}
			count++;
		}
		return samplesList;
	}

	public static List<Sample> readSamplesInDirectory(InputStream[] iss, int step) {
		return readSamplesInDirectory(iss, step, null);
	}

	public static List<Sample> readSamplesInDirectory(InputStream[] iss, int step, String group) {
		List<Sample> samplesAlighned = new ArrayList<Sample>();
		List<List<Sample>> samplesList = readSamplesList(iss, step, group);
		for (List<Sample> samples : samplesList) {
			for (Sample sample : samples) {
				samplesAlighned.add(sample);
			}
		}

		return samplesAlighned;
	}


	public static <T> void saveTxt(OutputStream os, T object) {
		try {
			OutputStreamWriter fw = newOutputStreamWriter(os);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(object.toString());
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}


	public static <T> void saveTxt(OutputStream os, T[] states) {
		try {
			OutputStreamWriter fw = newOutputStreamWriter(os);
			BufferedWriter bw = new BufferedWriter(fw);
			for (T state : states) {
				bw.write(state.toString() + "\n");
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}


	public static <T> void saveTxt(OutputStream os, List<T> list) {
		try {
			OutputStreamWriter fw = newOutputStreamWriter(os);
			BufferedWriter bw = new BufferedWriter(fw);
			for (T element : list) {
				bw.write(element + "\n");
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}


	public static void dumpToFile(Object obj, OutputStream os) {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(os);
			oos.writeObject(obj);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T restoreFromInputStream(InputStream is) {
		ObjectInputStream ois=null;
		try {
			ois = new ObjectInputStream(is);
			Object obj = ois.readObject();
			ois.close();
			return (T) obj;
		} catch (IOException ie) {
			ie.printStackTrace();
		} catch (ClassNotFoundException cfe) {
			cfe.printStackTrace();
		}
		return null;
	}

	public static String inputStreamToString(InputStream inputStream){
		BufferedReader br = null;
		try {
			br = new BufferedReader(newInputStreamReader(inputStream));
			StringBuffer sb = new StringBuffer();
			int c;
			while ((c = br.read()) != -1) {
				sb.append((char) c);
			}
			br.close();
			return sb.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static BLEBeaconConfig readCSVBLEBeaconConfiguration(InputStream is){
		BLEBeaconConfig iconf = new BLEBeaconConfig();

		try{
			Reader reader = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(reader);
			String line = br.readLine();
			while( (line=br.readLine()) !=null ){
				String[] tokens = line.split(",");
				for(String s : tokens){
					s.trim();
				}
				int major = Integer.parseInt(tokens[3]);
				int minor = Integer.parseInt(tokens[4]);
				int outputPower =Integer.parseInt(tokens[5]);
				int msdPower = Integer.parseInt(tokens[6]);
				iconf.putOutputPower(major, minor, outputPower);
				iconf.putMsdPower(major, minor, msdPower);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		return iconf;
	}

	public static FilenameFilter createFileExtensionFilter(final String extension) {
	    return new FilenameFilter() {
	        public boolean accept(File file, String name) {
	            boolean ret = name.endsWith(extension);
	            ret = ret && !name.startsWith(".");
	            return ret;
	        }
	    };
	}

	public static boolean hasBLEBeaconReadMethod(String readMethod){
		if(readMethod.equals(READ_JSON_BLEBEACON_LOCATION)){
			return true;
		}else{
			return false;
		}
	}
}
