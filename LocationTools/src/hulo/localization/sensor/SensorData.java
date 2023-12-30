/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation
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

package hulo.localization.sensor;

import java.util.ArrayList;
import java.util.List;

public class SensorData {

	long timestamp;
	String type;

	double[] data;
	public static final String PEDOMETER = "Pedometer";
	public static final String ORIENTATION_METER = "OrientationMeter";

	public void setTimestamp(long timestamp){
		this.timestamp = timestamp;
	}

	public long getTimestamp(){
		return timestamp;
	}

	public void setType(String type){
		this.type = type;
	}

	public String getType(){
		return type;
	}

	public void setData(double[] data){
		this.data = data;
	}

	public double[] getData(){
		return data;
	}

	public String toString(){
		String str = type+","+timestamp;
		int n = data.length;
		for(int i=0; i<n ; i++){
			str = str+","+data[i];
		}
		return str;
	}

	public String toCSVString(){
		String str = timestamp+","+type;
		int n = data.length;
		for(int i=0; i<n ; i++){
			str = str+","+data[i];
		}
		return str;
	}

	static final String beaconHet = "Beacon";
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
		long timestamp = Long.parseLong(tokens[0].trim());
		String type = tokens[1].trim();
		if(! type.equals(beaconHet) ){
			try{
				SensorData sensorData = new SensorData();
				sensorData.setTimestamp(timestamp);
				sensorData.setType(type);
				double[] data = new double[n-2];
				for(int j=2; j<n; j++){
					data[j-2] = Double.parseDouble(tokens[j]);
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

	public static List<SensorData> filter(List<SensorData> sensorDataList, String type){
		List<SensorData> listNew = new ArrayList<SensorData>();
		for(SensorData s: sensorDataList){
			String typeTmp = s.getType();
			if(typeTmp.equals(type) && typeTmp!=null){
				listNew.add(s);
			}
		}
		return listNew;
	}
}
