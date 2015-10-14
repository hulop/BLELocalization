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

package hulo.localization.project;

import hulo.localization.Beacon;
import hulo.localization.Sample;
import hulo.localization.data.DataUtils;
import hulo.localization.utils.ListUtils;
import hulo.localization.utils.ResourceUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class TrainDataReaderBase implements TrainDataReader{

	static final String TRAINING_KEY = "training";
	static final String READ_METHOD_KEY  = "readMethod";
	static final String MIN_RSSI_KEY = "minRssi";

	public static final String TRAINING_PATH = "/training";
	ResourceUtils resReader = ResourceUtils.getInstance();

	protected String toReadMethod(JSONObject formatJSON){
		try {
			String readMethod = formatJSON.getJSONObject(TRAINING_KEY).getString(READ_METHOD_KEY);
			return readMethod;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected Double toMinRssi(JSONObject formatJSON){
		Double minRssi = null;
		try {
			minRssi = formatJSON.getJSONObject(TRAINING_KEY).getDouble(MIN_RSSI_KEY);
			return minRssi;
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public boolean hasReadMethod(JSONObject formatJSON) {
		String readMethod = toReadMethod(formatJSON);
		if(DataUtils.hasReadMethod(readMethod)){
			return true;
		}
		return false;
	}

	@Override
	public TrainData read(String projectPath, JSONObject formatJSON, MapData mapData, String group) {
		setMinRssiToBeacon(formatJSON);
		TrainData trainData = loadTrainingData(projectPath, formatJSON, mapData, group);
		trainData = movingAveraging(trainData, formatJSON);
		return trainData;
	}

	protected void setMinRssiToBeacon(JSONObject formatJSON){
		Double minRssi = toMinRssi(formatJSON);
		if(minRssi!=null){
			Beacon.setMinRssi(minRssi);
			System.out.println("Beacon.minRssi="+Beacon.minRssi);
		}else{
			System.out.println("Beacon.minRssi="+Beacon.minRssi+" (default)");
		}
	}


	public TrainData loadTrainingData(String projectPath, JSONObject formatJSON, MapData mapData, String group){

		String trainDataDirPath = projectPath + TrainDataReaderBase.TRAINING_PATH;
		String readMethod = toReadMethod(formatJSON);

		List<String> resNames = resReader.getResourceNames(trainDataDirPath);

		TrainData trainData = null;
		if(DataUtils.hasReadMethod(readMethod)){
			trainData = new TrainData();
			trainData.setSamples(new ArrayList<Sample>());

			for(String resName : resNames){
				if(resName.endsWith(".json")){
					String[] splitted = resName.split("/");
					InputStream is = resReader.getInputStream(resName);
					//List<Sample> samples = DataUtils.readJSONSamples(is, group);
					List<Sample> samples = DataUtils.readSamplesInterface(is, readMethod, group);
					trainData.setSrcName(splitted[splitted.length-1]);
					trainData.getSamples().addAll(samples);
					System.out.println(splitted[splitted.length-1] + " is read by " + readMethod);
					System.out.println("trainData.samples.size()="+trainData.getSamples().size());
				}
			}
		}
		System.out.println("loadTrainingData finished.");

		return trainData;
	}

	protected TrainData movingAveraging(TrainData trainData, JSONObject formatJSON){
		try {
			if(formatJSON.getJSONObject("training").has("movingAverageWindow")){
				int mawindow = formatJSON.getJSONObject("training").getInt("movingAverageWindow");
				List<Sample> samples = trainData.getSamples();
				List<List<Sample>> samplesList = Sample.samplesToConsecutiveSamplesList(samples);
				List<List<Sample>> samplesListMA = Sample.movingAverageList(samplesList, mawindow);
				trainData.setSamples(ListUtils.flatten(samplesListMA));
				System.out.println("Training data moving averaged (window"+mawindow+")");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return trainData;
	}
}
