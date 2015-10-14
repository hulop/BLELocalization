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

import hulo.localization.models.sys.RandomWalkerOnFloorsMap;
import hulo.localization.utils.ClassUtils;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.wink.json4j.JSONObject;

public class PoseSetting{

	public static final String SIMPLE_NAME = PoseSetting.class.getSimpleName();

	// List of variables
	public static final String MEAN_STEP_LENGTH = "meanStepLength";
	public static final String STD_STEP_LENGTH = "stdStepLength";
	public static final String MIN_STEP_LENGTH = "minStepLength";
	public static final String MAX_STEP_LENGTH = "maxStepLength";

	public static final String STD_ORIENTATION = "stdOrientation";
	public static final String STD_ORIENTATION_BIAS = "stdOrientationBias";

	public static final String STD_N_STEPS = "stdNSteps";
	public static final String MIN_N_STEPS = "minNSteps";
	public static final String MAX_N_STEPS = "maxNSteps";

	public static final String RESET_RATE = "resetRate";

	public static final String MEAN_VELOCITY_REP = "meanVelocityRep";
	public static final String STD_VELOCITY_REP = "stdVelocityRep";
	public static final String DRIFT_VELOCITY_REP = "driftVelocityRep";
	public static final String MIN_VELOCITY_REP = "minVelocityRep";
	public static final String MAX_VELOCITY_REP = "maxVelocityRep";

	public static final String MEAN_RSSI_BIAS = "meanRssiBias";
	public static final String STD_RSSI_BIAS = "stdRssiBias";

	public static final String CONSTANTS_PRIOR = "constantsPrior";

	public static final String VELOCITY_MODE = "velocityMode";
	public static final String FLOOR_MOVE_MODE = RandomWalkerOnFloorsMap.FLOOR_MOVE_MODE; //"floorMoveMode";

	public static final String MAX_TRIAL = "maxTrial";

	// Type of distribution
	public static final String TRUNCATED_NORMAL_DISTRIBUTION = "truncatedNormalDistribution";
	public static final String UNIFORM_DISTRIBUTION = "uniformDistribution";

	// List of velocity computing methods
	public static final String CONSTANT_VELOCITY = "constantVelocity";
	public static final String STEPLENGTH_TIMES_NSTEPS = "stepLengthTimesNSteps";

	// List of floor move modes
	public static final String FLOORS_FREE = RandomWalkerOnFloorsMap.FLOORS_FREE; //"floorsFree"; // z changes +1 or -1 at small probability
	public static final String FLOORS_MAP = RandomWalkerOnFloorsMap.FLOORS_MAP; //"floorsMap";


	// Default values
	public double meanStepLength = 0.5;
	public double meanVelocityRep = 0.8;
	public double stdOrientation = Math.toRadians(1.0);
	public double stdOrientationBias =  Math.toRadians(1.0);
	public double stdStepLength = 0.1;
	public double stdNSteps = 0.1;
	public double stdVelocityRep = 0.1;
	public double driftVelocityRep = stdVelocityRep;
	public double resetRate = 0.0;

	public double minNSteps = -1.0;
	public double maxNSteps = 3.0; //Tentative value for testing
	public double minStepLength = 0.1;
	public double maxStepLength = 0.7; //Tentative value for testing
	public double minVelocityRep = 0.1;
	public double maxVelocityRep = 2.0;

	public double meanRssiBias = 0.0;
	public double stdRssiBias = 0.0;

	public String velocityMode = STEPLENGTH_TIMES_NSTEPS;
	public String constantsPrior = UNIFORM_DISTRIBUTION;
	public String floorMoveMode = FLOORS_FREE;

	public int maxTrial = 1;

	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(MEAN_STEP_LENGTH+"="+meanStepLength+",");
		sb.append(STD_ORIENTATION+"="+Math.toDegrees(stdOrientation)+"[degree],");
		sb.append(STD_ORIENTATION_BIAS+"="+Math.toDegrees(stdOrientationBias)+"[degree],");
		sb.append(STD_STEP_LENGTH+"="+stdStepLength+",");
		sb.append(STD_N_STEPS+"="+stdNSteps+",");
		sb.append(RESET_RATE+"="+resetRate+",");
		sb.append(VELOCITY_MODE+"="+velocityMode+",");
		return sb.toString();
	}

	public static PoseSetting createFromJSON(JSONObject json){
		System.out.println("Creating "+ PoseSetting.class.getSimpleName() +" from json");

		HashMap<String, Method> methodMap = new HashMap<String, Method>();
		try {
			methodMap.put("stdOrientation", Math.class.getMethod("toRadians", double.class));
			methodMap.put("stdOrientationBias", Math.class.getMethod("toRadians", double.class));
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
		} catch (SecurityException e1) {
			e1.printStackTrace();
		}

		PoseSetting setting = new PoseSetting();
		setting = (PoseSetting) ClassUtils.scanFieldsInJSON(setting, json, methodMap);

		if (!json.containsKey(DRIFT_VELOCITY_REP)) {
			System.out.println("driftVelocityRep was not found. driftVelocityRep=stdVelocityRep");
			setting.driftVelocityRep = setting.stdVelocityRep;
		}
		System.out.println("meanRssiBias="+setting.meanRssiBias+", stdRssiBias="+setting.stdRssiBias);

		return setting;
	}
}