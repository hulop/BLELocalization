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

import hulo.localization.BLEBeacon;
import hulo.localization.Beacon;
import hulo.localization.LocalizationInput;
import hulo.localization.LocalizationStatus;
import hulo.localization.Location;
import hulo.localization.Sample;
import hulo.localization.State;
import hulo.localization.beacon.AbstractBeaconFilter;
import hulo.localization.beacon.BeaconFilter;
import hulo.localization.beacon.BeaconFilterFactory;
import hulo.localization.kernels.GaussianKernel;
import hulo.localization.likelihood.LikelihoodModel;
import hulo.localization.likelihood.LikelihoodModelFactory;
import hulo.localization.models.ModelAdaptUtils;
import hulo.localization.thread.ExecutorServiceHolder;
import hulo.localization.utils.ArrayUtils;
import hulo.localization.utils.JSONUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class GaussianProcessLDPLMeanModel implements ObservationModel, Cloneable{

	BeaconFilter beaconFilter = new AbstractBeaconFilter();

	boolean doHyperParameterOptimize = true;
	double minRssi = Beacon.minRssi;
	double maxRssi = Beacon.maxRssi;

	GaussianProcessLDPLMean gpLDPL;

	List<Sample> samples;
	List<BLEBeacon> bleBeacons;
	double stdev=3.0;
	double lengthes[] = {1.0,1.0,0.1};
	double sigmaN = 3.0;

	double hDiff =0.0;

	public GaussianProcessLDPLMeanModel() {
		gpLDPL = new GaussianProcessLDPLMean();
	}


	public List<Sample> getSamples(){
		return samples;
	}

	public List<BLEBeacon> getBLEBeacons(){
		return bleBeacons;
	}

	public void setMinRssi(double minRssi){
		this.minRssi = minRssi;
	}
	public void setMaxRssi(double maxRssi){
		this.maxRssi = maxRssi;
	}

	public void setStdev(double stdev) {
		this.stdev = stdev;
	}
	public void setLengthes(double[] lengthes) {
		this.lengthes = lengthes;
	}
	public void setSigmaN(double sigmaN) {
		this.sigmaN = sigmaN;
	}

	public void setHDiff(double hDiff){
		this.hDiff = hDiff;
	}

	public void setParams(double[] params){
		gpLDPL.setParams(params);
	}
	public double[] getParams(){
		return gpLDPL.getParams();
	}


	public void setBLEBeacons(List<BLEBeacon> bleBeacons) {
		this.bleBeacons = bleBeacons;
	}

	public GaussianProcessLDPLMean getGaussianProcessLDPLMean(){
		return this.gpLDPL;
	}

	@Override
	public double[] getLogLikelihoods(List<Beacon> beacons, State[] locations) {
		List<Beacon> beaconsCleansed = Beacon.filterBeacons(beacons, minRssi, maxRssi);

		beaconsCleansed = beaconFilter.setBLEBeacon(bleBeacons).filter(beaconsCleansed, locations);
		BLEBeacon.setBLEBeaconIdsToMeasuredBeacons(bleBeacons, beaconsCleansed);
		int[] activeBeaconList = ModelAdaptUtils.beaconsToActiveBeaconArray(beaconsCleansed);

		final double[] ySub = ModelAdaptUtils.beaconsToVecSubset(beaconsCleansed);
		final double[][] X = ModelAdaptUtils.locationsToMat(locations);

		// Adjust bias by average bias
		final double[] rssiBiases = ModelAdaptUtils.biasesToVec(locations);

		double logLLs[] = null;
		try {
			Class<?> cls = gpLDPL.getClass();
			Constructor<?> cst = cls.getConstructor(gpLDPL.getClass());
			final GaussianProcessLDPLMean gpLDPLtmp = (GaussianProcessLDPLMean) cst.newInstance(gpLDPL);
			gpLDPLtmp.updateByActiveBeaconList(activeBeaconList);
			int n = X.length;
			final double logpro[] = new double[n];
			Future<?>[] futures = new Future<?>[n];
			for(int i=0; i<n; i++){
				final int idx = i;
				futures[idx] = ExecutorServiceHolder.getExecutorService().submit(new Runnable() {
					public void run() {
						// Subtract bias from an observation vector.
						double[] ySubAdjusted = ArrayUtils.addScalar(ySub, -rssiBiases[idx]);
						logpro[idx] = gpLDPLtmp.logProbaygivenx(X[idx], ySubAdjusted);
					}
				});
			}
			for(int i=0; i < n; i++) {
				futures[i].get();
			}
			logLLs = logpro;
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return logLLs;
	}


	public List<Beacon> predictBeacons(State state, List<Beacon> beacons){
		double[] x = ModelAdaptUtils.locationToVec(state);
		int[] activeBeaconList = ModelAdaptUtils.beaconsToActiveBeaconArray(beacons);
		double[] ypred;
		synchronized(gpLDPL){
			gpLDPL.updateByActiveBeaconList(activeBeaconList);
			ypred = gpLDPL.predictygivenx(x);
		}

		List<Beacon> beaconsPred = new ArrayList<Beacon>();
		for(int i=0; i<activeBeaconList.length; i++){
			int major = -1;
			int minor = activeBeaconList[i];
			double rssi = ypred[i];
			Beacon beacon = new Beacon(major, minor, (float) rssi);
			beaconsPred.add(beacon);
		}

		return beaconsPred;
	}

	private void setupGPLDPL(){
		gpLDPL.setHDiff(hDiff);
		gpLDPL.setSigmaN(sigmaN);
		GaussianKernel kernel = new GaussianKernel();
		kernel.setStdev(stdev);
		kernel.setLengthes(lengthes);
		gpLDPL.setKernel(kernel);
	}


	int[] activeBeaconList;
	public void setActiveBeaconList(int[] activeBeaconList){
		this.activeBeaconList = activeBeaconList;
		gpLDPL.setActiveBeaconList(activeBeaconList);
	}

	public void train(List<Sample> samples){
		BLEBeacon.setBLEBeaconIdsToSamples(bleBeacons, samples);
		List<List<Sample>> samplesList = Sample.samplesToConsecutiveSamplesList(samples);
		if(doHyperParameterOptimize){
			doHyperParameterOptimize = false;
			trainWithHyperParameters(samplesList);
		}
		List<Sample> samplesAvaragedTrain = Sample.meanTrimList(samplesList, minRssi);
		this.trainByAveragedSamples(samplesAvaragedTrain);
	}

	protected void trainByAveragedSamples(List<Sample> samples){
		int nBeacons = bleBeacons.size();
		BLEBeacon.setBLEBeaconIdsToSamples(bleBeacons, samples);
		setupGPLDPL();
		this.samples=samples;
		double[][] X = ModelAdaptUtils.samplesToLocationsMat(samples);
		double[][] Y = ModelAdaptUtils.samplesToBeaconsListMat(samples, minRssi, nBeacons);
		double[][] bLocs = ModelAdaptUtils.BLEBeaconsToMat(bleBeacons, nBeacons);
		gpLDPL.setSourceLocs(bLocs);

		int[] actBecList = new int[nBeacons];
		for(int i=0; i<nBeacons; i++) actBecList[i]=i;
		gpLDPL.setActiveBeaconList(actBecList);

		gpLDPL.fit(X, Y);
	}

	protected void trainWithHyperParameters(List<List<Sample>> samplesListTrainParent){
		List<Sample> samplesAvaragedTrain = Sample.meanTrimList(samplesListTrainParent, minRssi);
		this.train(samplesAvaragedTrain);
		// Update active beacon list about actually observed beacons
		Sample samplesAllAverage = Sample.mean(samplesAvaragedTrain, minRssi);
		List<Beacon> observedBeacons = samplesAllAverage.getBeacons();
		this.setActiveBeaconList(ModelAdaptUtils.beaconsToActiveBeaconArray(observedBeacons));

		double stdevTmp = stdev;
		this.setStdev(0.0); // stdev is set to be zero before optimization of LDPL model
		this.optimizeForLDPLModel();

		this.setStdev(stdevTmp); // stdev must be updated before optimization of GP
		this.optimizeForGPIsoScaleLOOMSE(); // optimize for lx(=ly) and stdev
		this.optimizeEstimateSigmaN(Sample.samplesListToSamples(samplesListTrainParent)); // estimate sigmaN

		this.train(samplesAvaragedTrain);
	}

	public JSONObject toJSON(){
		JSONObject json = new JSONObject();

		JSONObject LDPLJSON = new JSONObject();
		JSONObject GPJSON = new JSONObject();
		JSONObject optJSON = new JSONObject();

		try {
			double[] params = getParams();
			LDPLJSON.put("n", params[0]).put("A", params[1]).put("fa", params[2]).put("fb", params[3]).put(HDIFF, hDiff);

			GPJSON.put("lengthes", new Double[]{lengthes[0], lengthes[1], lengthes[2]});
			GPJSON.put("sigmaP", stdev).put("sigmaN", sigmaN).put("useMask", gpLDPL.getUseMask()?1:0).put("constVar", gpLDPL.getOptConstVar());

			optJSON.put("optimizeHyperParameters", doHyperParameterOptimize?1:0);

			json.put(LDPLParameters, LDPLJSON);
			json.put(GPParameters, GPJSON);
			json.put(OPTIONS, optJSON);

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return json;
	}

	public static final String LDPLParameters = "LDPLParameters";
	public static final String GPParameters = "GPParameters";

	public static final String LIKELIHOOD_MODEL = "LikelihoodModel";
	public static final String BEACON_FILTER = "BeaconFilter";

	static final String HDIFF = "heightDifference";
	static final String OPTIONS = "options";

	public static final String ARRAYS = "arrays";


	JSONObject jsonParams = null;
	public void settingFromJSON(JSONObject json){
		jsonParams = json;
		try {
			JSONObject ldplParams = json.getJSONObject(LDPLParameters);
			JSONObject gpParams = json.getJSONObject(GPParameters);
			JSONObject optJSON = json.getJSONObject(OPTIONS);

			//LDPLParameters
			double n = ldplParams.getDouble("n");
			double A = ldplParams.getDouble("A");
			double fa = ldplParams.getDouble("fa");
			double fb = ldplParams.getDouble("fb");
			if(ldplParams.containsKey(HDIFF)){
				double hDiff = ldplParams.getDouble(HDIFF);
				this.setParams(new double[]{n,A,fa,fb});
				this.setHDiff(hDiff);
			}

			//GPParamters
			JSONArray jarray = gpParams.getJSONArray("lengthes");
			lengthes = JSONUtils.array1DfromJSONArray(jarray);
			stdev = gpParams.getDouble("sigmaP");
			sigmaN = gpParams.getDouble("sigmaN");

			boolean useMask = (gpParams.getInt("useMask") == 1);
			int optConstVar = gpParams.getInt("constVar");

			gpLDPL.setUseMask(useMask);
			gpLDPL.setOptConstVar(optConstVar);

			// Options
			doHyperParameterOptimize = optJSON.optBoolean("optimizeHyperParameters");
			if(! doHyperParameterOptimize){
				doHyperParameterOptimize = optJSON.optInt("optimizeHyperParameters")==1;
			}

			// Optional variables
			if(gpParams.containsKey(ARRAYS)){
				JSONObject jobjArrays = gpParams.getJSONObject(ARRAYS);
				gpLDPL.fromJSON(jobjArrays);
			}
			if(json.containsKey(LIKELIHOOD_MODEL)){
				JSONObject likelihoodJSON = json.getJSONObject(LIKELIHOOD_MODEL);
				LikelihoodModel likelihoodModel = LikelihoodModelFactory.create(likelihoodJSON);
				gpLDPL.setLikelihoodModel(likelihoodModel);
			}else{
				System.out.println("The key for ["+LIKELIHOOD_MODEL +"] was not found.");
			}

			if(json.containsKey(BEACON_FILTER)){
				JSONObject bfJSON = json.getJSONObject(BEACON_FILTER);
				BeaconFilter bf = BeaconFilterFactory.create(bfJSON);
				beaconFilter = bf;
			}else{
				System.out.println("The key for ["+BEACON_FILTER +"] was not found.");
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	MultivariateFunction generateLDPLObjectiveFunction(final GaussianProcessLDPLMeanModel model, final double[] initPoint){

		MultivariateFunction negaLLfunc = new MultivariateFunction(){
			@Override
			public double value(double[] point){
				double[] params = gpLDPL.getParams();
				for(int i=0; i<initPoint.length; i++){
					params[i] = point[i];
				}
				gpLDPL.setParams(params);
				model.train(samples);
				gpLDPL.updateByActiveBeaconList(activeBeaconList);
				double logLikelihood = gpLDPL.looPredLogLikelihood();
				double looMSE = gpLDPL.looMSE();

				StringBuilder sb = new StringBuilder();
				sb.append("optimizeForLDPL: n="+params[0]+",A="+params[1]+",fa="+params[2]+",fb="+params[3]);
				sb.append(", LOOMSE="+looMSE);
				sb.append(", logLikelihood="+(-logLikelihood));
				System.out.println(sb.toString());
				
				return -logLikelihood;
			}
		};
		return negaLLfunc;
	}


	void optimizeForLDPL(){
		double[] params = gpLDPL.getParams();
		double[] pointInit = {params[0], params[1]};
		double[] dPointInit = {Math.abs(params[0])/10.0,
								Math.abs(params[1])/10.0
								};
		
		MultivariateFunction negaLLfunc = generateLDPLObjectiveFunction(this, pointInit);
		minimize(negaLLfunc, pointInit, dPointInit);
	}


	void optimizeForLDPLFloorDiff(){
		double[] params = gpLDPL.getParams();
		double[] pointInit = {params[0], params[1], params[2], params[3]};
		double[] dPointInit = {Math.abs(params[0])/10.0,
								Math.abs(params[1])/10.0,
								Math.abs(params[2])/10.0,
								Math.abs(params[3])/10.0
							};
		MultivariateFunction negaLLfunc = generateLDPLObjectiveFunction(this, pointInit);
		minimize(negaLLfunc, pointInit, dPointInit);
	}
	
	void optimizeForLDPLModel(){
		if(isSingleFloor(samples)){
			optimizeForLDPL();
		}else{
			optimizeForLDPLFloorDiff();
		}
	}

	boolean isSingleFloor(List<Sample> samples){
		Set<Double> floorSet = new HashSet<>();
		for(Sample s: samples){
			Location loc = s.getLocation();
			double z = loc.getZ();
			floorSet.add(z);
		}
		if(floorSet.size()==1){
			return true;
		}else{
			return false;
		}
	}

	PointValuePair minimize(MultivariateFunction func, double[] pointInit, double[] diffPointInit){
		return GaussianProcessLDPLMean.minimize(func, pointInit, diffPointInit);
	}

	MultivariateFunction generateGPIsoScaleLOOMSE(final GaussianProcessLDPLMeanModel model){
		MultivariateFunction negaLLfunc = new MultivariateFunction(){
			@Override
			public double value(double[] point){
				double lengthx = Math.pow(10, point[0]);
				double lambda = Math.pow(10, point[1]);

				double stdev = model.stdev;
				double lz = model.lengthes[2];
				model.lengthes = new double[]{lengthx, lengthx, lz};
				model.sigmaN = Math.sqrt(lambda)*stdev;

				model.train(samples);
				gpLDPL.updateByActiveBeaconList(activeBeaconList);

				double looMSE = gpLDPL.looMSE();
				StringBuilder sb = new StringBuilder();
				sb.append("optimizeForGPLOOError: lx=ly="+lengthx+",lz="+lz+",lambda="+lambda+",looMSE="+looMSE);
				System.out.println(sb.toString());

				return looMSE;
			}
		};
		return negaLLfunc;
	}


	void optimizeForGPIsoScaleLOOMSE(){
		MultivariateFunction negaLLfunc = generateGPIsoScaleLOOMSE(this);
		double lambda0 = Math.pow(sigmaN/stdev, 2);
		double[] pointInit = { Math.log10(lengthes[0]), Math.log10(lambda0)};
		double[] dPointInit = {0.01, 0.01};
		minimize(negaLLfunc, pointInit, dPointInit);
	}



	public void optimizeEstimateSigmaN(final List<Sample> samplesForEstimateSigmaN){
		optimizeEstimateSigmaN_NM(samplesForEstimateSigmaN);
	}

	void optimizeEstimateSigmaN_NM(final List<Sample> samplesForEstimateSigmaN){
		MultivariateFunction negaLLfunc = generateSigmaNObjectiveFunction(this, samplesForEstimateSigmaN);
		double[] pointInit = {Math.log10(sigmaN)};
		double[] dPointInit = {0.01};
		minimize(negaLLfunc, pointInit, dPointInit);
	}

	MultivariateFunction generateSigmaNObjectiveFunction(final GaussianProcessLDPLMeanModel model,final List<Sample> allSamples){

		MultivariateFunction negaLLfunc = new MultivariateFunction(){
			@Override
			public double value(double[] point){
				double ratioConst = Math.pow(model.stdev/model.sigmaN, 2);
				model.sigmaN = Math.pow(10, point[0]);
				model.stdev = Math.sqrt(ratioConst)*model.sigmaN;

				model.train(samples);
				gpLDPL.updateByActiveBeaconList(activeBeaconList);

				double logLikelihood = 0;
				State[] statesTmp = new State[1];
				for(Sample sample: allSamples){
					State state = sample.getLocation();
					List<Beacon> beacons = sample.getBeacons();
					statesTmp[0] = state;
					double LL = getLogLikelihoods(beacons, statesTmp)[0];
					logLikelihood += LL;
				}

				StringBuilder sb = new StringBuilder();
				sb.append("optimizeEstimateSigmaN: lx="+model.lengthes[0]+",ly="+model.lengthes[1]+",stdev="+stdev+",sigmaN="+sigmaN+",value="+(-logLikelihood));
				System.out.println(sb.toString());
				return -logLikelihood;
			}
		};
		return negaLLfunc;
	}

	void optimize(){
		optimizeForLDPL();
		optimizeForGPIsoScaleLOOMSE();
	}

	@Override
	public LocalizationStatus update(LocalizationStatus status,
			LocalizationInput input) {
		System.err.println("update is not supported in "+GaussianProcessLDPLMeanModel.class.getSimpleName());
		return null;
	}

}
