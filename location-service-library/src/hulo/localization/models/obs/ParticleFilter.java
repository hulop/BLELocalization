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

package hulo.localization.models.obs;
import hulo.localization.Beacon;
import hulo.localization.LocalizationInput;
import hulo.localization.LocalizationStatus;
import hulo.localization.Location;
import hulo.localization.Pose;
import hulo.localization.Sample;
import hulo.localization.State;
import hulo.localization.models.sys.PoseSystemModel;
import hulo.localization.models.sys.RandomWalker;
import hulo.localization.models.sys.SystemModel;
import hulo.localization.models.sys.SystemModelWithSensor;
import hulo.localization.pf.ParticlePreprocessor;
import hulo.localization.pf.ParticlePreprocessorProvider;
import hulo.localization.pf.ResamplerProvider;
import hulo.localization.utils.ArrayUtils;
import hulo.localization.utils.StatUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class ParticleFilter implements ObservationModel{

	static final String N_PARTICLES = "nParticles";
	static final String AVERAGING_TYPE = "averagingType";
	static final String SIMPLE_AVERAGE= "simpleAverage";
	static final String WEIGHTED_AVERAGE = "weightedAverage";
	static final String RESAMPLING_METHOD = "resamplingMethod";
	static final String ALPHA_WEAKEN = "alphaWeaken";

    SystemModel systemModel;
    ObservationModel observationModel;

    Random random;
    StringBuilder sb = new StringBuilder();

    List<Sample> trainingSamples;

    int numParticles;
    String averagingType = SIMPLE_AVERAGE;
    boolean usesWeighted = false;

    String resamplingMethodName = "systematicResampling";

	double alphaWeaken = 1.0;

	ParticlePreprocessor particlePreprocessor = null;

    public int getNumParticles(){
    	return numParticles;
    }

    public void setNumParticles(int numParticles){
    	this.numParticles = numParticles;
    }

    ParticleFilter(){}

    public ParticleFilter(SystemModel systemModel, ObservationModel observationModel){
        initializeRandom();
        this.systemModel=systemModel;
        this.observationModel=observationModel;
    }

    void initializeRandom(){
    	random = new Random();
    }

    public void settingFromJSON(JSONObject json){
    	// Parse json
    	try {
			Integer np = json.getInt(N_PARTICLES);
			setNumParticles(np);
			if(json.containsKey(AVERAGING_TYPE)){
				averagingType = json.getString(AVERAGING_TYPE);
				System.out.println(AVERAGING_TYPE + " is set to " + averagingType);
			}
			usesWeighted = averagingType.equals(WEIGHTED_AVERAGE)? true : false;
			if(json.containsKey(RESAMPLING_METHOD)){
				String str = json.getString(RESAMPLING_METHOD);
				resamplingMethodName = str;
				System.out.println(RESAMPLING_METHOD + " is set to " + resamplingMethodName);
			}
			alphaWeaken = json.containsKey(ALPHA_WEAKEN) ? json.getDouble(ALPHA_WEAKEN) : 1.0;
			System.out.println(this.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}

    	// Set up particle preprocessor
    	JSONObject prepJSON = json.optJSONObject(ParticlePreprocessor.PARTICLE_PREPROCESSOR);
    	if(prepJSON!=null){
    		ParticlePreprocessorProvider provider = new ParticlePreprocessorProvider();
    		if(provider.hasFactory(prepJSON)){
    			particlePreprocessor = provider.provide(prepJSON, this);
    			System.out.println(particlePreprocessor.toString() + " is created.");
    		}
    	}else{
    		System.out.println("The key for ["+ParticlePreprocessor.PARTICLE_PREPROCESSOR+"] was not found.");
    	}

    }

    public SystemModel getSystemModel(){
    	return systemModel;
    }

    public ObservationModel getObservationModel(){
    	return observationModel;
    }

    LocalizationStatus initializeLocalizationStatus(LocalizationInput locInput){
    	return initializeLocalizationStatus();
    }

    LocalizationStatus initializeLocalizationStatus(){
    	List<Location> initLocs = Sample.generateRandomLocationsResample(trainingSamples, numParticles);
    	LocalizationStatus locStatus = new LocalizationStatus();
    	locStatus.setStates(initLocs.toArray(new Location[0]));
    	return locStatus;
    }

    LocalizationStatus resetLocalizationStatus(Location locInitial){
    	State[] states = new State[numParticles];
    	for(int i=0; i<numParticles; i++){
    		states[i] = locInitial.clone();
    		states[i].setWeight(1.0/((double) numParticles));
    	}
    	LocalizationStatus locStatus = new LocalizationStatus();
    	locStatus.setLocation(locInitial);
    	locStatus.setStates(states);
    	return locStatus;
    }


    LocalizationStatus resetStatus(LocalizationInput locInput){
		Location locInitial = locInput.getLocationInitial();
		if(locInitial!=null){
			System.out.println("locInitial="+locInitial);
			LocalizationStatus locStatus = resetLocalizationStatus(locInitial);
			return locStatus;
		}else{
			System.out.println("Localization status was cleared.");
			return null;
		}
    }


    LocalizationStatus correctStatus(LocalizationStatus locStatus, LocalizationInput locInput){
    	double smallStdev = 1e-6;

    	Location locationTrue = locInput.getLocationInitial();
    	Location stdevLocationTrue = locInput.getStdevLocationTrue();
    	State[] states = locStatus.getStates();

    	State meanState = null;
    	try {
    		Class<?> cls = states[0].getClass();
			Method mtd = cls.getMethod("mean", State[].class, boolean.class);
			meanState = (State) mtd.invoke(null, states, usesWeighted);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}

    	double x_true = locationTrue.getX();
    	double y_true = locationTrue.getY();
    	double z_true = locationTrue.getZ();
    	double s_x = stdevLocationTrue.getX();
    	double s_y = stdevLocationTrue.getY();
    	double s_z = stdevLocationTrue.getZ();

    	if(s_x<smallStdev) s_x += smallStdev;
    	if(s_y<smallStdev) s_y += smallStdev;
    	if(s_z<smallStdev) s_z += smallStdev;

    	// Set true loc
    	meanState.setX((float) x_true);
    	meanState.setY((float) y_true);
    	meanState.setZ((float) z_true);

    	int n = states.length;
    	State[] statesNew = new State[n+1];
    	double[] logLikelihoods = new double[n+1];

    	for(int i=0; i<n+1 ; i++){
    		if(i<n){
    			statesNew[i] = states[i];
    		}else{
    			statesNew[i] = meanState;
    		}
    	}

    	for(int i=0; i<n+1; i++){
    		State s = statesNew[i];
    		logLikelihoods[i] = StatUtils.logProbaNormal(s.getX(), x_true, s_x)
    							+ StatUtils.logProbaNormal(s.getY(), y_true, s_y)
    							+ StatUtils.logProbaNormal(s.getZ(), z_true, s_z);
    	}

    	double[] weights = computeWeights(logLikelihoods);
    	List<State> statesList = Arrays.asList(doResampling(statesNew, weights));
    	statesNew = statesList.toArray(new State[0]);

    	states = Arrays.copyOfRange(statesNew, 0, n);

    	states = assignWeights(states);

    	locStatus.setLocation(Location.mean(states, usesWeighted));
    	locStatus.setStates(states);

    	return locStatus;
    }


    LocalizationStatus doPreprocess(LocalizationStatus locStatus, LocalizationInput locInput){
 		if(locStatus==null && locInput.getBeacons()!=null){
			locStatus = initializeLocalizationStatus(locInput);
		}
    	if(locInput.getDoReset()){
			locStatus = resetStatus(locInput);
		}
		if(locInput.getDoCorrect()){
			locStatus = correctStatus(locStatus, locInput);
		}
		return locStatus;

    }


    @Override
    public LocalizationStatus update(LocalizationStatus locStatus, LocalizationInput locInput){
			// Preprocess
    		sb.setLength(0);
    		locStatus = doPreprocess(locStatus, locInput);
    		if(locInput.getBeacons()==null){
    			return locStatus;
    		}

    		long start,end;

            start = System.currentTimeMillis();
    		State[] states = locStatus.getStates();

    		// Assign weights
    		states = assignWeights(states);

    		// Shuffle
            states = shuffleStates(states);
            numParticles = states.length;

            // Predict
            states = predict(states, locInput);
            if (states == null) {
            	return update((LocalizationStatus) null, locInput); // Reset input
            }

            // Preprocess if required.
            if(particlePreprocessor!=null){
            	states = particlePreprocessor.process(states, locInput);
            }

            end = System.currentTimeMillis();
            sb.append("Time(prediction)="+(end-start));

            // Evaluate weights of particles
            start = System.currentTimeMillis();
            double weights[] = getResamplingWeights(locInput, states);
            // likelihood * weight
            for(int i=0; i<weights.length; i++){
            	weights[i] = weights[i]*states[i].getWeight();
            }

            weights = normalizeWeight(weights);
            states = assignWeights(states, weights);

            end =  System.currentTimeMillis();

            sb.append(", Time(resampling)="+(end-start));

            if(usesWeighted){
            	locStatus = updateMeanState(locStatus, states, usesWeighted);
            }

            // Do resampling
            states = doResampling(states, weights);
            // Assign average weight after resampling
            states = assignWeights(states);

            // Shuffle
            states = shuffleStates(states);

            if(! usesWeighted){
            	locStatus = updateMeanState(locStatus, states, usesWeighted);
            }

            locStatus.setStates(states);

            System.out.println(sb.toString());
            sb.setLength(0);

            return locStatus;
    }

    State[] assignWeights(State[] states){
    	int n = states.length;
    	double w = 1.0/n;
    	for(State s: states){
    		s.setWeight(w);
    	}
    	return states;
    }

    State[] assignWeights(State[] states, double[] weights){
    	for(int i=0; i<states.length; i++){
    		states[i].setWeight(weights[i]);
    	}
    	return states;
    }

    protected LocalizationStatus updateMeanState(LocalizationStatus locStatus, State[] states, boolean usesWeighted){
    	locStatus.setLocation(Location.mean(states, usesWeighted));
    	if(systemModel instanceof PoseSystemModel){
    	   locStatus.setMeanState(Pose.mean(states, usesWeighted));
    	   sb.append(", mean(states)="+ ( (Pose) locStatus.getMeanState()).toJSONObject().toString());
        }else if (systemModel instanceof RandomWalker){
        	sb.append(", mean(Location)="+((Location) locStatus.getLocation()).toJSONObject().toString());
        }
    	return locStatus;
    }


    State[] predict(State[] states){
    	return systemModel.predict(states);
    }

    State[] predict(State[] states, LocalizationInput locInput){
    	if(systemModel instanceof SystemModelWithSensor){
    		return ((SystemModelWithSensor) systemModel).predict(states, locInput);
    	}else{
    		return predict(states);
    	}
    }

    State[] shuffleStates(State[] states){
    	List<State> stateList = Arrays.asList(states);
    	Collections.shuffle(stateList, random);
    	states = stateList.toArray(new State[states.length]);
    	return states;
    }

    double[] getResamplingWeights(LocalizationInput locInput, State[] states){
        double[] logLikelihoods = getLogLikelihoods(locInput, states);
        logLikelihoods = weakenLogLikelihood(logLikelihoods);
        double[] weights = computeWeights(logLikelihoods);
        return weights;
    }

    protected double[] weakenLogLikelihood(double[] logLikelihoods){
    	int n = logLikelihoods.length;
    	double[] logLLsNew = new double[n];
    	for(int i=0; i<n; i++){
    		logLLsNew[i] = alphaWeaken*logLikelihoods[i];
    	}
    	return logLLsNew;
    }

    protected double[] computeWeights(double[] logLikelihoods){
        // Find max loglikelihoods
        double maxLogLikelihood = ArrayUtils.max(logLikelihoods);

        // compute resampling weights
        double weights[] = new double[logLikelihoods.length];
        double sum_weights=0.0;
        for(int i=0; i<weights.length; i++){
            weights[i] = Math.exp(logLikelihoods[i] - maxLogLikelihood);
            sum_weights += weights[i];
        }
        for(int i=0; i<weights.length; i++){
            weights[i] = weights[i]/sum_weights;
        }
        return weights;
    }

    @Override
	public double[] getLogLikelihoods(List<Beacon> beacons, State[] locations){
        return observationModel.getLogLikelihoods(beacons, locations);
    }

    public double[] getLogLikelihoods(LocalizationInput locInput, State[] locations){
    	if(observationModel instanceof ObservationModelWithSensor){
    		return ((ObservationModelWithSensor) observationModel).getLogLikelihoods(locInput, locations);
    	}else{
    		return observationModel.getLogLikelihoods(locInput.getBeacons(), locations);
    	}
    }

    double[] normalizeWeight(double[] weights){
    	int n = weights.length;
    	double[] weightsNormed = new double[weights.length];
    	double sum = 0;
    	for(int i=0; i<n; i++){
    		sum += weights[i];
    	}
    	for(int i=0; i<n; i++){
    		weightsNormed[i] = weights[i]/sum;
    	}
    	return weightsNormed;
    }

	State[] doResampling(State[] states, double[] weights ){
    	return ResamplerProvider.getResampler(resamplingMethodName).resampling(states, weights);
    }

    public String toString(){
    	return "ParticleFilter["+systemModel+","+observationModel+"], N="+numParticles+", alphaWeaken="+alphaWeaken;
    }

    public void print(){
    	System.out.println(this);
    }

	@Override
	public void train(List<Sample> trainingSamples) {
		this.trainingSamples = trainingSamples;
	}

}
