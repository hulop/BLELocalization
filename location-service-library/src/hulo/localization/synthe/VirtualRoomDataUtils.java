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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class VirtualRoomDataUtils {

	public static List<BLEBeacon> readMapBLEBeacons(InputStream is){
		int z = 0;
		List<BLEBeacon> bleBeacons = new ArrayList<>();

		try {
			JSONObject json = (JSONObject) JSON.parse(is);

			String type = json.getString("type");
			if(type.equals("FeatureCollection")){
				JSONArray features = json.getJSONArray("features");
				Iterator<JSONObject> iter = features.iterator();
				while(iter.hasNext()){
					JSONObject feature = iter.next();
					JSONObject properties = feature.getJSONObject("properties");
					JSONObject geometry = feature.getJSONObject("geometry");
					type = properties.getString("type");
					if(type.equals("beacon")){

						int major = Integer.parseInt(properties.getString("major"));
						int minor = Integer.parseInt(properties.getString("minor"));
						String uuid = properties.getString("uuid");
						int outPower = Integer.parseInt(properties.getString("power"));
						int interval = Integer.parseInt(properties.getString("interval"));

						double x = geometry.getJSONArray("coordinates").getInt(0);
						double y = geometry.getJSONArray("coordinates").getInt(1);

						BLEBeacon bleBeacon = new BLEBeacon(minor, major, minor, x, y, z);
						bleBeacon.setUuid(uuid);
						bleBeacon.setOutputPower(outPower);

						bleBeacons.add(bleBeacon);
					}
				}
			}
		} catch (NullPointerException | JSONException e) {
			e.printStackTrace();
		}

		return bleBeacons;

	}

	public static List<Wall> readWalls(InputStream is){

		int z = 0;

		List<Wall> walls = new ArrayList<>();

		try {
			JSONObject json = (JSONObject) JSON.parse(is);

			String type = json.getString("type");
			if(type.equals("FeatureCollection")){
				JSONArray features = json.getJSONArray("features");
				Iterator<JSONObject> iter = features.iterator();
				while(iter.hasNext()){
					JSONObject feature = iter.next();
					JSONObject properties = feature.getJSONObject("properties");
					JSONObject geometry = feature.getJSONObject("geometry");
					type = properties.getString("type");
					if(type.equals("wall")){

						double height = Double.parseDouble(properties.getString("height"));
						double attenuation = Double.parseDouble(properties.getString("decay"));

						JSONArray coordsArray = geometry.getJSONArray("coordinates");
						int npoints = coordsArray.size();

						for(int i=0; i<npoints-1; i++){
							JSONArray p0 = coordsArray.getJSONArray(i);
							JSONArray p1 = coordsArray.getJSONArray(i+1);

							Wall wall = new Wall.Builder(p0.getDouble(0), p0.getDouble(1), p1.getDouble(0), p1.getDouble(1))
											.height(height)
											.attenuation(attenuation)
											.build();

							walls.add(wall);
						}
					}
				}
			}
		} catch (NullPointerException | JSONException e) {
			e.printStackTrace();
		}

		return walls;

	}


	static JSONArray parseFeatures(JSONObject json){
		try{
			String type = json.getString("type");
			if(type.equals("FeatureCollection")){
				JSONArray features = json.getJSONArray("features");
				return features;
			}
		}catch(JSONException e){
			e.printStackTrace();
		}
		return null;
	}

	public static List<Location> readWalkLocations(InputStream is){
		int z = 0;
		List<Location> locs = new ArrayList<>();
		try {
			JSONObject json = (JSONObject) JSON.parse(is);
			String type = json.getString("type");
			if(type.equals("FeatureCollection")){
				JSONArray features = json.getJSONArray("features");
				Iterator<JSONObject> iter = features.iterator();
				while(iter.hasNext()){
					JSONObject feature = iter.next();
					JSONObject properties = feature.getJSONObject("properties");
					JSONObject geometry = feature.getJSONObject("geometry");
					type = properties.getString("type");
					if(type.equals("walk")){

						double height = Double.parseDouble(properties.getString("height"));
						int repeat = Integer.parseInt(properties.getString("repeat"));

						JSONArray coordinates = geometry.getJSONArray("coordinates");

						Iterator<JSONArray> iterCoords = coordinates.iterator();
						while(iterCoords.hasNext()){
							JSONArray coord = iterCoords.next();

							double x = coord.getDouble(0);
							double y = coord.getDouble(1);

							Location loc = new Location(x, y, z);
							loc.setH((float) height);

							for(int t=0; t<repeat; t++){
								locs.add(loc);
							}
						}
					}
				}
			}
		} catch (NullPointerException | JSONException e) {
			e.printStackTrace();
		}
		return locs;
	}



	public static List<Location> readGridLocations(InputStream is){

		int z = 0;

		List<Location> locs = new ArrayList<>();
		try {
			JSONObject json = (JSONObject) JSON.parse(is);
			String type = json.getString("type");
			if(type.equals("FeatureCollection")){
				JSONArray features = json.getJSONArray("features");
				Iterator<JSONObject> iter = features.iterator();
				while(iter.hasNext()){
					JSONObject feature = iter.next();
					JSONObject properties = feature.getJSONObject("properties");
					JSONObject geometry = feature.getJSONObject("geometry");
					String propertiesType = properties.getString("type");
					String geometryType = geometry.getString("type");
					if(propertiesType.equals("grid")){
						double height = Double.parseDouble(properties.getString("height"));
						int repeat = Integer.parseInt(properties.getString("repeat"));
						int spacing = Integer.parseInt(properties.getString("interval"));

						JSONArray coordinates = geometry.getJSONArray("coordinates");
						Iterator<JSONArray> iterCoords = null;
						if(geometryType.equals("Polygon")){
							JSONArray exteriorRing = coordinates.getJSONArray(0);
							iterCoords = exteriorRing.iterator();
						}else if(geometryType.equals("LineString")){
							iterCoords = coordinates.iterator();
						}

						double minx = Double.MAX_VALUE;
						double miny = Double.MAX_VALUE;
						double maxx = -Double.MAX_VALUE;
						double maxy = -Double.MAX_VALUE;
						while(iterCoords.hasNext()){
							JSONArray coord = iterCoords.next();
							double x = coord.getDouble(0);
							double y = coord.getDouble(1);
							minx = Math.min(minx, x);
							miny = Math.min(miny, y);
							maxx = Math.max(maxx, x);
							maxy = Math.max(maxy, y);
						}

						double x = minx;
						while(x<=maxx){
							double y = miny;
							while(y<=maxy){
								Location loc = new Location(x, y, z);
								loc.setH((float) height);
								for(int t=0; t<repeat; t++){
									locs.add(loc);
								}
								y+=spacing;
							}
							x+=spacing;
						}
					}
				}
			}
		} catch (NullPointerException | JSONException e) {
			e.printStackTrace();
		}
		return locs;
	}
}
