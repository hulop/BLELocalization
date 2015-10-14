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

import java.util.List;

public class LocalizationInput {

	String uuid;
	long timestamp;
	List<Beacon> beacons;

	// A fiele to store additinal sensor data.
	List<SensorData> sensorDataList;

	public enum UpdateMode{update, reset, correct};

	// Fields for reset
	Location locTrue = null;
	Double orientationTrue = null;

	// Fields for correct
	UpdateMode updateMode = UpdateMode.update;
	Location stdevLocTrue = null;

	public LocalizationInput(){};

	public LocalizationInput(LocalizationInput input){
		LocalizationInput inputNew = new LocalizationInput();
		inputNew.uuid = input.uuid;
		inputNew.timestamp = input.timestamp;
		inputNew.beacons = input.beacons;
		inputNew.sensorDataList = input.sensorDataList;
		inputNew.updateMode = input.updateMode;
		inputNew.locTrue = input.locTrue;
		inputNew.orientationTrue = input.orientationTrue;
	}

	@Deprecated
	public boolean getDoReset(){
		boolean doReset = (this.updateMode==UpdateMode.reset);
		return doReset;
	}

	@Deprecated
	public void setDoReset(boolean doReset){
		if(doReset){
			this.updateMode = UpdateMode.reset;
		}
	}

	@Deprecated
	public boolean getDoCorrect(){
		boolean doCorrect = (this.updateMode==UpdateMode.correct);
		return doCorrect;
	}

	@Deprecated
	public void setDoCurrect(boolean doCorrect){
		if(doCorrect){
			this.updateMode = UpdateMode.correct;
		}
	}

	public LocalizationInput setUpdateMode(UpdateMode updateMode){
		this.updateMode = updateMode;
		return this;
	}


	public UpdateMode getUpdateMode(){
		return updateMode;
	}

	public LocalizationInput setInitialLocation(double x, double y, double z){
		locTrue = new Location(x, y, z);
		return this;
	}

	public LocalizationInput setInitialOrientation(double orientation){
		orientationTrue = orientation;
		return this;
	}

	public LocalizationInput setStdevLocTrue(double x, double y, double z){
		stdevLocTrue = new Location(x, y, z);
		return this;
	}

	public Location getLocationInitial(){
		return locTrue;
	}
	public Double getOrientationInitial(){
		return orientationTrue;
	}

	public Location getStdevLocationTrue(){
		return stdevLocTrue;
	}

	public List<SensorData> getSensorDataList() {
		return sensorDataList;
	}
	public LocalizationInput setSensorDataList(List<SensorData> sensorDataList) {
		this.sensorDataList = sensorDataList;
		return this;
	}

	public void setTimestamp(long timestamp){
		this.timestamp = timestamp;
	}
	public long getTimestamp(){
		return timestamp;
	}

	public String getUuid() {
		return uuid;
	}

	public LocalizationInput setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	public List<Beacon> getBeacons() {
		return beacons;
	}
	public LocalizationInput setBeacons(List<Beacon> beacons) {
		this.beacons = beacons;
		return this;
	}

}
