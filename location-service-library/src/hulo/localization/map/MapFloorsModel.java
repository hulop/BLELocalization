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

package hulo.localization.map;

import hulo.localization.State;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class MapFloorsModel implements FloorModel{

	public static final String MAP_FLOORS_MODEL = "mapFloorsModel";

	HashMap<Integer, ImageHolder> maps = new HashMap<Integer, ImageHolder>();
	Map<Integer, CoordinateSystem> coordSyss = new HashMap<Integer, CoordinateSystem>();

	int[] floors= {0}; // Elements in floors are assumed to be sorted in ascending order.

	Color colorWall = Color.black;
	Color colorStairs = Color.blue;
	Color colorElevator = Color.yellow;
	Color colorOutside = Color.green;

	protected Random random;

	// Parameter to control floor transition
	double proba_stay = 0.5;
	double proba_up   = 0.25;
	double proba_down = 0.25;

	public MapFloorsModel() {
		initialize();
	}

	static final String PROBA_UP = "probaUp";
	static final String PROBA_DOWN = "probaDown";

	public void settingFromJSON(JSONObject json){
		try{
			proba_up = json.containsKey(PROBA_UP) ? json.getDouble(PROBA_UP) : 0.25;
			proba_down = json.has(PROBA_DOWN) ? json.getDouble(PROBA_DOWN) : 0.25;
			proba_stay = 1.0 - proba_up - proba_down;
		}catch(JSONException e){
			e.printStackTrace();
		}
	}

	protected void initialize(){
		initializeRandom();
	}

	protected void initializeRandom(){
		random = new Random();
	}

	public void putMap(Integer integer, ImageHolder map){
		maps.put(integer, map);
	}

	public ImageHolder getMap(Integer integer){
		return maps.get(integer);
	}

	public void putCoordinateSystem(Integer integer, CoordinateSystem coordSys){
		coordSyss.put(integer, coordSys);
	}


	public void setFloors(int[] floors){
		Arrays.sort(floors); // sort in ascending order
		this.floors = floors;
	}


	public int[] getFloors(){
		return floors;
	}

	public State worldToLocalState(State state){
		int z = (int) state.getZ();
		CoordinateSystem coordSys = coordSyss.get(z);
		if(coordSys == null){
			return null;
		}
		State localState = coordSys.worldToLocalState(state);
		return localState;
	}

	public State localToWorldState(State state){
		int z = (int) state.getZ();
		return coordSyss.get(z).localToWorldState(state);
	}

	private int nextInt(int n){
		return random.nextInt(n);
	}

	private double nextDouble(){
		return random.nextDouble();
	}

	public boolean isMovable(State prevState, State newState){
		if(!isMovablePoint(newState)){
			return false;
		}
		if(isCrossingWall(prevState, newState)){
			return false;
		}
		return true;
	}

	@Override
	public boolean isStairs(State state){
		if(! isMovablePoint(state)){
			return false;
		}
		if(getColor(state).equals(colorStairs)){
			return true;
		}
		return false;
	}

	@Override
	public boolean isElevator(State state){
		if(! isMovablePoint(state)){
			return false;
		}
		if(getColor(state).equals(colorElevator)){
			return true;
		}
		return false;
	}

	@Override
	public boolean isOutside(State state){
		if(! isValidPoint(state)){
			return true;
		}
		if(getColor(state).equals(colorOutside)){
			return true;
		}
		return false;
	}

	public boolean isWall(State state){
		if(getColor(state).equals(colorWall)){
			return true;
		}else{
			return false;
		}
	}

	private Color getColor(State state){
		State localState = worldToLocalState(state);
		int x = (int) (localState.getX());
		int y = (int) (localState.getY());
		int z = (int) (localState.getZ()); //floor
		ImageHolder map = maps.get(z);
		Color c = map.get(x, y);
		return c;
	}

	@Override
	public boolean isValidPoint(State state){
		if(! isValidFloor(state)){
			return false;
		}
		State localState = worldToLocalState(state);
		int x = (int) (localState.getX());
		int y = (int) (localState.getY());
		int z = (int) (localState.getZ()); //floor
		ImageHolder map = maps.get(z);

		int rows = map.rows();
		int cols = map.cols();

		if(0<=x && x<cols && 0<=y && y<rows ){
			return true;
		}else{
			return false;
		}
	}

	public boolean isValidFloor(State state){
		int z = (int) state.getZ();
		CoordinateSystem coordSys = coordSyss.get(z);
		if(coordSys == null){
			return false;
		}else{
			return true;
		}
	}

	public boolean isMovablePoint(State state){
		if(!isValidPoint(state)){
			return false;
		}
		if(isWall(state)){
			return false;
		}else{
			return true;
		}
	}

	public boolean isCrossingWall(State prevState, State newState){
		return wallCrossingRatio(prevState, newState) < 1.0;
	}

	public double wallCrossingRatio(State prevState, State newState){
		State prevStateLocal = worldToLocalState(prevState);
		State newStateLocal = worldToLocalState(newState);

		double x0 = prevStateLocal.getX();
		double y0 = prevStateLocal.getY();
		double z0 = prevStateLocal.getZ();

		double x1 = newStateLocal.getX();
		double y1 = newStateLocal.getY();
		double z1 = newStateLocal.getZ();

		if(z0!=z1){
			return 0;
		}

		ImageHolder map = maps.get((int) z0);

		double norm = Math.sqrt(Math.pow(x1-x0, 2) +Math.pow(y1-y0, 2));

		int norm_int = (int) norm + 1;

		double dx = (x1-x0)/norm_int;
		double dy = (y1-y0)/norm_int;

		double x=x0;
		double y=y0;

		int count=0;
		while(count<=norm_int){

			if(map.get((int) x, (int) y).equals(colorWall)){
				return ((double)count-1)/norm_int;
			}
			x+=dx;
			y+=dy;
			count++;
		}
		return ((double)count-1)/norm_int;
	}

	public State predictOnFloorsMap(State state){
		State stateNew = null;
		if(isStairs(state)){
			stateNew = moveInterFloor(state);
		}else if(isElevator(state)){
			stateNew = moveElevator(state);
		}
		else{
			stateNew = state;
		}
		return stateNew;
	}

	public State moveInterFloor(State state){
		int z_min = floors[0];
		int z_max = floors[floors.length-1];
		int z = (int) state.getZ();
		int z_new = z;

		State state_new = null;
		while(true){
			double p = nextDouble();
			if(p<proba_stay){
				z_new = z;
			}else if(p<(proba_stay+proba_up)){
				z_new = z+1;
			}else if(p<(proba_stay+proba_up+proba_down)){
				z_new = z-1;
			}
			if(z_min<=z_new && z_new<=z_max){
				state_new = (State) state.clone();
				state_new.setZ(z_new);
				if(isMovablePoint(state_new) && isStairs(state_new)){
					break;
				}
			}
		}
		return state_new;
	}

	public State moveElevator(State state){
		int z_min = floors[0];
		int z_max = floors[floors.length-1];
		State state_new = null;
		while(true){
			int z_new = z_min + nextInt(z_max-z_min + 1);
			state_new = (State) state.clone();
			state_new.setZ(z_new);
			if(isMovablePoint(state_new) && isElevator(state_new)){
				break;
			}
		}
		return state_new;
	}

	public boolean successMove(State prevState, State newState){
		return true;
	}

	public void print(){
		for(int floor: floors){
			ImageHolder map = maps.get(floor);
			System.out.println("image:"+map.cols()+" x "+map.rows());
		}
	}

	public double estimateWallAngle(State prevState, State newState) {
		double r = this.wallCrossingRatio(prevState, newState);
		double dx = newState.getX() - prevState.getX();
		double dy = newState.getY() - prevState.getY();
		double x = prevState.getX() + dx * r;
		double y = prevState.getY() + dy * r;
		double a = Math.atan2(dy, dx);

		State nearWall = (State) prevState.clone();
		nearWall.setX((float)x);
		nearWall.setY((float)y);
		State nearWall2 = (State) newState.clone();
		int sign = 1;
		for(double angle = 1; angle <= 60; angle++) {
			for(int i=0; i < 2; i++) {
				sign *= -1;
				double newAngle = a + sign * Math.toRadians(angle);
				double x2 = x + Math.cos(newAngle) * dx;
				double y2 = y + Math.sin(newAngle) * dy;
				nearWall2.setX((float)x2);
				nearWall2.setY((float)y2);
				if (this.wallCrossingRatio(nearWall, nearWall2) >= 1.0) {
					return newAngle;
				}
			}
		}
		return Double.NaN;
	}

}
