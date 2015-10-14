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

package hulo.localization.synthe;

import hulo.localization.BLEBeacon;
import hulo.localization.Location;
import hulo.localization.Sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class SyntheticDataGenerator {

	SyntheticBeaconDataGenerator beaconGen;
	SyntheticMotionDataGenerator motionGen;

	public static  SyntheticDataGenerator create(JSONObject json){

		SyntheticDataGenerator gen = new SyntheticDataGenerator();

		try{
			JSONObject beaconJSON = json.getJSONObject("Beacon");
			JSONObject motionJSON = json.getJSONObject("Motion");

			gen.beaconGen = new SyntheticBeaconDataGenerator(beaconJSON);
			gen.motionGen = new SyntheticMotionDataGenerator(motionJSON);

			JSONObject randomJSON = json.getJSONObject("Random");
			int seed =  randomJSON==null ? 1234 : randomJSON.getInt("seed");
			gen.beaconGen.setRandom(new Random(seed));
			gen.motionGen.setRandom(new Random(seed));

		}catch(JSONException e){
			e.printStackTrace();
		}
		return gen;
	}

	public static String parseOptions(String[] args){
		String generatorSettingPath = null;
		int argc = args.length;
		for(int i=0; i<argc; i++){
			String s = args[i];
			switch(s){
			case "-i":
				i++;
				generatorSettingPath = args[i];
			}
		}
		return generatorSettingPath;
	}

	static final String DEFAULT_GENERATOR_SETTING_PATH = "../location-service-resource/example/generatorSetting002.json";

	public static void main(String[] args) throws IOException, NullPointerException, JSONException {

		int argc = args.length;

		String generatorSettingPath = DEFAULT_GENERATOR_SETTING_PATH;

		if(argc>0){
			String str = parseOptions(args);
			if(str!=null){
				generatorSettingPath = str;
			}
		}
		System.out.println("generatorSettingPath="+generatorSettingPath);

		InputStream is = new FileInputStream(generatorSettingPath);
		JSONObject json = (JSONObject) JSON.parse(is);

		SyntheticDataGenerator gen = SyntheticDataGenerator.create(json);

		// Parse JSON
		JSONObject dataJSON = json.optJSONObject("Data");
		String projectPath = dataJSON.optString("project");
		String mapName = dataJSON.optString("map");
		String trainName = dataJSON.optString("train");
		String testName = dataJSON.optString("test");

		// Map file
		String mapPath =  projectPath +"/map/"+ mapName;
		File mapFile = new File(mapPath);
		if(! mapFile.exists()) throw new RuntimeException( mapFile.getName() + " was not found");

		// Training and test data directories
		String trainDir = projectPath +"/train/";
		String testDir = projectPath + "/test/";
		String trainPath = trainDir + trainName;
		String testPath = testDir+ testName;
		File trainDirFile = new File(trainDir);
		File testDirFile = new File(testDir);
		if(! trainDirFile.exists()) trainDirFile.mkdir();
		if(! testDirFile.exists()) testDirFile.mkdir();

		is = new FileInputStream(mapFile);
		List<Location> walkLocations = VirtualRoomDataUtils.readWalkLocations(is);
		is = new FileInputStream(mapFile);
		List<BLEBeacon> bleBeacons = VirtualRoomDataUtils.readMapBLEBeacons(is);
		is = new FileInputStream(mapFile);
		List<Wall> walls = VirtualRoomDataUtils.readWalls(is);
		is = new FileInputStream(mapFile);
		List<Location> gridLocations = VirtualRoomDataUtils.readGridLocations(is);

		SyntheticBeaconDataGenerator beaconGen = gen.beaconGen;
		SyntheticMotionDataGenerator motionGen = gen.motionGen;

		// Set virtual room data.
		beaconGen.fit(gridLocations, bleBeacons, walls);

		// Generate training data.
		List<Sample> trainSamples = beaconGen.generateSamples(gridLocations);
		JSONArray jarray = Sample.samplesToJSONArray(trainSamples);

		// Generate test data.
		List<Sample> testSamples = beaconGen.generateSamples(walkLocations);
		// Generate motion and append to test data.
		motionGen.setWalkingSamples(testSamples);
		List<Sample> testSamplesWithSensors = motionGen.generate();
		String csv = Sample.samplesToCSVString(testSamplesWithSensors);

		// Save training data
		FileWriter trainFw = new FileWriter(new File(trainPath));
		trainFw.write(jarray.toString());
		trainFw.close();
		// Save test data
		FileWriter testWithSensorFw = new FileWriter(new File(testPath));
		testWithSensorFw.write(csv);
		testWithSensorFw.close();

		System.out.println("Training data were saved to "+trainPath);
		System.out.println("Test data were saved to "+ testPath);

	}


}
