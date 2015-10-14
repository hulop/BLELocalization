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

import java.util.Iterator;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class LocalizationStatus{

    Location location = null;
    State[] states = null;
    State meanState = null;
    public String info = null;
    JSONObject jsonAdditional = new JSONObject();

	// Options
	static boolean outputStates = false;
	public static void setOutputStates(boolean outputStates){
		LocalizationStatus.outputStates = outputStates;
	}

    public void setLocation(Location location){
        this.location = location;
    }
    public void setStates(State[] states){
        this.states = states;
    }

    public Location getLocation(){
        return location;
    }

    public void setMeanState(State state){
    	this.meanState = state;
    }

    public State getMeanState(){
    	return meanState;
    }

    public State[] getStates(){
        return states;
    }

    public boolean hasLocation(){
    	return (location!=null);
    }

    public boolean hasStates(){
        return (states!=null);
    }

    public String toString(){
    	String str;
    	str = "LocalizationStatus{Location="+location+", states +"+"}";
    	return str;
    }

    public void putJSONAdditional(String key, Object value){
    	try {
			jsonAdditional.put(key, value);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    }

    JSONObject joinJSONAdditional(JSONObject inJSON){
    	try{
    		Iterator<String> iter = jsonAdditional.keys();
    		while(iter.hasNext()){
    			String key = iter.next();
    			inJSON.put(key, jsonAdditional.get(key));
    		}
    	}catch (JSONException e){
    		e.printStackTrace();
    	}
    	return inJSON;
    }


    public JSONObject toJSONObject(){
    	try{
    		JSONArray statesJSONArray = null;
    		Location stdevState = null;
    		Location stdevAxis = null;
    		if(states != null && outputStates){
    			statesJSONArray = Location.statesToJSONArray(states);
    		}
    		if(states != null){
    			stdevState = Location.std(states);
    		}
    		JSONObject json = null;
    		if(meanState!=null){
    			if(meanState instanceof Pose){
    				json = ((Pose) meanState).toJSONObject();
    			}else if(meanState instanceof Location){
    				json = ((Location) meanState).toJSONObject();
    			}else{
    				System.out.println("Unknown state object in localization status.");
    			}
    			if(stdevAxis == null && states!=null){
    				stdevAxis = Pose.stdVelocityAxis(states);
    				json.put("stdevAxial", stdevAxisToJSONObject(stdevAxis));
    			}
    		}else if(location!=null){
    			json = location.toJSONObject();
    		}
			if(stdevState!=null){
				json.put("stdev", stdevState.toJSONObject());
			}
			if(statesJSONArray!=null){
				json.put("states", statesJSONArray);
			}
			json = joinJSONAdditional(json);
    		return json;
    	}catch(JSONException e){
    		e.printStackTrace();
    	}
    	return null;
    }

    JSONObject stdevAxisToJSONObject(State state){
    	JSONObject json = new JSONObject();
    	double x = state.getX();
    	double y = state.getY();
    	try {
			json.put("vertical", x);
			json.put("horizontal", y);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	return json;
    }

}
