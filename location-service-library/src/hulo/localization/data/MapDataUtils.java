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

import hulo.localization.map.CoordinateSystem;
import hulo.localization.map.ImageHolder;
import hulo.localization.map.MapFloorsModel;
import hulo.localization.utils.ArrayUtils;
import hulo.localization.utils.ResourceUtils;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class MapDataUtils {

	static final String FLOORS_KEY = "floors";

	static final String ORIGIN_X = "origin_x";
	static final String ORIGIN_Y = "origin_y";
	static final String PUNIT = "punit";
	static final String MARGIN_X = "margin_x";
	static final String MARGIN_Y = "margin_y";

	@SuppressWarnings("unchecked")
	static <T> T  uncheckedCast(Object obj){
		return (T) obj;
	}

	public static Map<String, Integer> parseJSONFloors(JSONArray floorsArray){
		Map<String, Integer> map = new LinkedHashMap<String, Integer>();
		try {
			Iterator<JSONObject> iter = uncheckedCast(floorsArray.iterator());
			while(iter.hasNext()){
				JSONObject jobj = iter.next();
				String floor = jobj.getString("floor");
				Integer floor_num  = jobj.getInt("floor_num");
				map.put(floor, floor_num);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return map;
	}

	private static InputStream newFloorsIS(String mapDir){
		String floorsPath = mapDir + "/floors.json";
		InputStream floorsIS = ResourceUtils.getInstance().getInputStream(floorsPath);
		return floorsIS;
	}

	private static InputStream newFloorImageIS(String mapDir, int floorNum){
		String imgDir = mapDir + "/WallMap/floor_num_"+floorNum+".png";
		InputStream inSt = ResourceUtils.getInstance().getInputStream(imgDir);
		return inSt;
	}

	public static MapFloorsModel readMapFloorsModel(String mapDir){

		double origin_x = 0;
		double origin_y = 0;
		double punit = 0;
		double margin_x;
		double margin_y;

		Map<String, Integer> floorTofloorNumMap = null;

		try {
			InputStream floorsIS = newFloorsIS(mapDir);
			JSONObject floorsJSON = (JSONObject) JSON.parse(floorsIS);
			JSONObject paramsJSON = floorsJSON.getJSONObject("parameters");
			origin_x = paramsJSON.getDouble(ORIGIN_X);
			origin_y = paramsJSON.getDouble(ORIGIN_Y);
			punit = paramsJSON.getDouble(PUNIT);
			margin_x = paramsJSON.getDouble(MARGIN_X);
			margin_y = paramsJSON.getDouble(MARGIN_Y);

			JSONArray floorsArray = floorsJSON.getJSONArray(FLOORS_KEY);
			floorTofloorNumMap = parseJSONFloors(floorsArray);

		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// Sort elements of floorNums in ascending order.
		Set<Integer> floorNumSet = new TreeSet<Integer>(floorTofloorNumMap.values());
		int[] floorNums = ArrayUtils.toIntArray(floorNumSet.toArray(new Integer[0]));

		MapFloorsModel mapFloorsModel = new MapFloorsModel();
		mapFloorsModel.setFloors(floorNums);

		for(int floorNum: floorNums){
			InputStream inSt = newFloorImageIS(mapDir, floorNum);
			ImageHolder map = new ImageHolder(inSt);
			CoordinateSystem coordSys = new CoordinateSystem(punit, -punit, origin_x, origin_y);
			mapFloorsModel.putMap(floorNum, map);
			mapFloorsModel.putCoordinateSystem(floorNum, coordSys);
			System.out.println("floor="+floorNum+", map="+map);
		}
		return mapFloorsModel;
	}

}
