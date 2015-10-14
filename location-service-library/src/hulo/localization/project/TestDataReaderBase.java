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
import hulo.localization.utils.ResourceUtils;

import java.io.InputStream;
import java.util.List;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class TestDataReaderBase implements TestDataReader {

	static final String TEST_KEY = "test";
	static final String READ_METHOD_KEY = TrainDataReaderBase.READ_METHOD_KEY;
	static final String MIN_RSSI_KEY = TrainDataReaderBase.MIN_RSSI_KEY;

	public static final String CSV_EXT = ".csv";

	public static final String TEST_PATH = "/test";

	protected ResourceUtils resReader = ResourceUtils.getInstance();

	protected String toReadMethod(JSONObject formatJSON){
		String readMethod = null;
		try {
			readMethod = formatJSON.getJSONObject(TEST_KEY).getString(READ_METHOD_KEY);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return readMethod;
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
	public TestData read(String projectPath, JSONObject formatJSON, MapData mapData, String group) {
		readAndSetMinRssi(formatJSON);
		TestData testData = loadTestData(projectPath, formatJSON, mapData, group);
		testData = movingAverageTestData(testData, formatJSON);

		return testData;
	}

	protected void readAndSetMinRssi(JSONObject formatJSON){
		try {
			Double minRssi = formatJSON.getJSONObject(TEST_KEY).getDouble(MIN_RSSI_KEY);
			Beacon.setMinRssi(minRssi);
			System.out.println("Beacon.minRssi="+Beacon.minRssi);
		} catch (JSONException e) {
			System.out.println("Beacon.minRssi="+Beacon.minRssi+" (default)");
		}
	}


	public TestData loadTestData(String projectPath, JSONObject formatJSON, MapData mapData, String group){
		String testDataDir = projectPath + TestDataReaderBase.TEST_PATH;
		List<String> resNames = resReader.getResourceNames(testDataDir);
		String readMethod = toReadMethod(formatJSON);
		TestData testData = new TestData();
		if(DataUtils.hasReadMethod(readMethod)){
			for(String str: resNames){
				if(str.endsWith(CSV_EXT)){
					InputStream is = resReader.getInputStream(str);
					String[] strs = str.split("/");
					String fileName = strs[strs.length-1];
					List<Sample> samples = DataUtils.readCSVWalkingSamples(is, group);
					DataUnit data = new DataUnit(fileName, samples);
					testData.add(data);
					System.out.println(fileName + " is read by " + readMethod);
				}
			}
		}
		return testData;
	}


	public TestData movingAverageTestData(TestData testData, JSONObject formatJSON) {
		try {
			if (formatJSON.getJSONObject("test").has("movingAverageWindow")) {
				int mawindow = formatJSON.getJSONObject("test").getInt("movingAverageWindow");
				int nTest = testData.getSize();
				System.out.println("Test data moving averaged (window" + mawindow + ")");
				for (int i = 0; i < nTest; i++) {
					DataUnit tst = testData.get(i);
					List<Sample> smps = Sample.movingAverage(tst.getSamples(), mawindow);
					tst.setSamples(smps);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return testData;
	}


}
