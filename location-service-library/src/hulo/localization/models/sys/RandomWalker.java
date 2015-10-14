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

import hulo.localization.Location;
import hulo.localization.State;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class RandomWalker implements SystemModel{
    // Random Walk model

	static final double SIGMA_DEFAULT=1.5;

    protected double sigma;
    protected State[] states;
    protected State locationExpected;

    boolean isWalking = true;
    public void setIsWalking(boolean isWalking){
    	this.isWalking = isWalking;
    }

    boolean useSensorData = false;

    public boolean isUseSensorData() {
		return useSensorData;
	}
	public void setUseSensorData(boolean useSensorData) {
		this.useSensorData = useSensorData;
	}

	private static Random random = new Random(); //Random number generator

	public static Random getRandom() {
		return random;
	}
	public static void setRandom(Random random) {
		RandomWalker.random = random;
	}

    public RandomWalker(){
        initialize(SIGMA_DEFAULT);
    }

    public RandomWalker(double sigma){
        initialize(sigma);
    }

    public RandomWalker(InputStream inputStream){
    	initialize(inputStream);
    }

    public void settingFromJSON(JSONObject json){
		try {
			double sigma = json.getDouble("sigma");
			setSigma(sigma);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	void initialize(double sigma){
    	setSigma(sigma);
    	initializeRandom();
    }

    void initialize(InputStream inputStream){
		Properties config = new Properties();
	    try {
			config.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	    sigma = Double.parseDouble(config.getProperty("sigma"));
		initialize(sigma);
	}

    protected void initializeRandom(){
    	setRandom(new Random());
    }

    public void setSigma(double sigma){
    	this.sigma = sigma;
    }
    public double getSigma(){ return sigma; }

    public void setStates(State[] states){
        this.states = states;
    }
    public State[] getStates(){
        return states;
    }

    public void predict(){
    	states = predict(states);
    }

	@Override
    public State[] predict(State[] states){
        int nStates = states.length;
    	State[] statesNew = new State[nStates];
        for(int i=0; i<nStates; i++){
        	State state = states[i];
        	statesNew[i] = predict(state);
        }
        return statesNew;
    }

	public State predict(State state){
        double x = state.getX();
        double y = state.getY();
        double z = state.getZ();
        // Assuming 2D random walk
        x += sigma*nextGaussian();
        y += sigma*nextGaussian();
        State stateNew = (State) state.clone();
        stateNew.setX((float) x);
        stateNew.setY((float) y);
        return stateNew;
	}


	protected double nextGaussian(){
		return getRandom().nextGaussian();
	}

	protected double nextDouble(){
		return getRandom().nextDouble();
	}

    void computeLocationExpected(){
        locationExpected = Location.mean(states);
    }

    public State getLocationExpected(){
        computeLocationExpected();
        return locationExpected;
    }

    public String toString(){
    	return this.getClass().getSimpleName()+"("+parametersToString()+")";
    }

    public String parametersToString(){
    	return "sigma="+sigma;
    }

    public void print(){
    	System.out.println(this);
    }

}