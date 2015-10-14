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

package hulo.localization.models.sys;

import hulo.localization.State;
import hulo.localization.map.MapFloorsModel;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class RandomWalkerOnFloorsMap extends RandomWalker implements SystemModel, SystemModelOnFloorsMap{
	MapFloorsModel mapFloorsModel = null;

	public static final String FLOORS_MAP = "floorsMap";
	public static final String FLOORS_FREE = "floorsFree";

	public static final String FLOOR_MOVE_MODE = "floorMoveMode";

	String floorMoveMode = FLOORS_MAP;

	public RandomWalkerOnFloorsMap() {
	}

	public RandomWalkerOnFloorsMap(double sigma) {
		super(sigma);
	}


	@Override
	public void settingFromJSON(JSONObject json){
		super.settingFromJSON(json);
		try {
			String floorMoveMode = json.getString(FLOOR_MOVE_MODE);
			this.floorMoveMode = floorMoveMode;
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void setMapFloorsModel(MapFloorsModel mapFloorsModel){
		this.mapFloorsModel = mapFloorsModel;
	}

	public MapFloorsModel getMapFloorsModel(){
		return mapFloorsModel;
	}

	@Override
    public State[] predict(State[] states){
        int nStates = states.length;
    	State[] statesNew = new State[nStates];
        int count=0;
        for(int i=0; count<nStates; i++){
        	State statePrev = states[i%nStates];
        	// Filter out the cases where the statePrev is not a movable point.
        	if(mapFloorsModel.isMovablePoint(statePrev)){
        		State stateNew = predict(statePrev);
        		statesNew[count] = stateNew;
        		count++;
        	}
        }
        return statesNew;
    }

	double sigmaStairs = 0.5;
	double sigmaElevator = 0.5;

	@Override
	public State predict(State state){
		if(floorMoveMode.equals(FLOORS_MAP)){
			State newState;
			if(mapFloorsModel.isStairs(state)){
				State tempState = mapFloorsModel.moveInterFloor(state);
				while(true){
					newState = super.predict(tempState);
					if(mapFloorsModel.isMovable(tempState, newState)){
						break;
					}
				}
				return newState;
			}else if(mapFloorsModel.isElevator(state)){
				State tempState = mapFloorsModel.moveElevator(state);
				while(true){
					newState = super.predict(tempState);
					if(mapFloorsModel.isMovable(tempState, newState)){
						break;
					}
				}
				return newState;
			}else{
				State tempState = state;
				while(true){
					newState = super.predict(tempState);
					if(mapFloorsModel.isMovable(tempState, newState)){
						break;
					}
				}
				return newState;
			}
		}

		else if(floorMoveMode.equals(FLOORS_FREE)){
			State newState = null;
			State tempState = null;
			while(true){
				if(mapFloorsModel.isOutside(state)){
					tempState = state.clone();
				}else{
					tempState = RandomWalkerOnFloorsFree.predictZ(state, isWalking);
				}
				if(! mapFloorsModel.isMovablePoint(tempState)){
					continue;
				}
				newState = RandomWalkerOnFloorsFree.predict2D(tempState, sigma);
				if(mapFloorsModel.isMovable(tempState, newState)){
					break;
				}
			}
			return newState;
		}

		else {
			return null;
		}
	}

	public String parametersToString(){
		String str = super.parametersToString();
		str = str + "," + FLOOR_MOVE_MODE+"="+floorMoveMode;
		return str;
	}

}
