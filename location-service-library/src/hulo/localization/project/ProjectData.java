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

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;


public class ProjectData {

	static final String UUID = "UUID";
	static final String SITE_ID = "site_id";

	public static class Info{
		String uuid;
		String site_id;

		public Info(JSONObject info){
			try{
				String uuid = info.getString(UUID);
				String site_id = info.getString(SITE_ID);
				this.uuid(uuid);
				this.siteId(site_id);
			} catch(JSONException e){
				e.printStackTrace();
			}
		}

		public String uuid() {
			return uuid;
		}
		public void uuid(String uuid) {
			this.uuid = uuid;
		}
		public String siteId() {
			return site_id;
		}
		public void siteId(String site_id) {
			this.site_id = site_id;
		}

	}

	Info info;
	TrainData trainData;
	TestData testData;
	MapData mapData;

	public ProjectData(){
		trainData = new TrainData();
		testData = new TestData();
		mapData = new MapData();
	}

	public ProjectData info(Info info){
		this.info = info;
		return this;
	}

	public Info info(){
		return info;
	}

	public TrainData trainData() {
		return trainData;
	}

	public ProjectData trainData(TrainData trainData) {
		this.trainData = trainData;
		return this;
	}

	public TestData testData() {
		return testData;
	}

	public ProjectData testData(TestData testData) {
		this.testData = testData;
		return this;
	}

	public MapData mapData() {
		return mapData;
	}

	public ProjectData mapData(MapData mapData) {
		this.mapData = mapData;
		return this;
	}

}
