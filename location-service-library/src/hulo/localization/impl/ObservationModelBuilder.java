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

package hulo.localization.impl;

import hulo.localization.LocalizationInput;
import hulo.localization.LocalizationStatus;
import hulo.localization.Sample;
import hulo.localization.map.MapFloorsModel;
import hulo.localization.models.ModelBuilderCore;
import hulo.localization.models.obs.ObservationModel;
import hulo.localization.project.MapData;
import hulo.localization.project.MapDataReader;
import hulo.localization.project.ProjectData;
import hulo.localization.project.SampleVisualizer;
import hulo.localization.project.TestData;
import hulo.localization.project.TestDataReader;
import hulo.localization.project.TrainData;
import hulo.localization.project.TrainDataReader;
import hulo.localization.thread.ExecutorServiceHolder;
import hulo.localization.utils.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class ObservationModelBuilder {

	ObservationModel model;
	ProjectData projData;

	String projectPath;
	String modelPath;

	String projectName;
	String modelName;
	String modelProjectDir;
	String resultDir;
	String resultFilePath;

	public String getUUID(){
		return projData.info().uuid();
	}
	public String getSiteId(){
		return projData.info().siteId();
	}

	ResourceUtils resReader = ResourceUtils.getInstance();
	String resoureceType;

	// Options
	boolean readsTest = false;
	boolean optVisualize = false;
	boolean optRefresh = false;

	public ObservationModelBuilder(){
		projData = new ProjectData();
	}

	public String getModelPath(){
		return this.modelPath;
	}

	public String getProjectPath(){
		return this.projectPath;
	}

	public void setObservationModel(ObservationModel model){
		this.model = model;
	}

	public ObservationModel getObservationModel(){
		return model;
	}

	public MapFloorsModel getMapFloorsModel(){
		MapData mapData = projData.mapData();
		return (MapFloorsModel) mapData.getObjects().get(MapFloorsModel.MAP_FLOORS_MODEL);
	}

	public ObservationModelBuilder setReadsTest(boolean readsTest){
		this.readsTest = readsTest;
		return this;
	}

	public boolean getReadsTest(){
		return readsTest;
	}

	public void parseOptions(String[] args){
		int argc = args.length;
		if(argc < 4){
			printHelp();
		}else{
			for(int i=0; i<argc; i++){
				String arg = args[i];
				if(arg.equals("-m")){
					this.modelPath = args[i+1];
				}
				if(arg.equals("-p")){
					this.projectPath = args[i+1];
				}
				if(arg.equals("-v") || arg.equals("--visualize")){
					this.optVisualize = true;
				}
				if(arg.equals("-r") || arg.equals("--refresh")){
					this.optRefresh = true;
				}
				if(arg.equals("--resouce")){
					this.resoureceType = args[i+1];
				}
			}
		}

		setProjectPathAndModelPath(this.projectPath, this.modelPath);
		System.out.println("options [refresh="+optRefresh+",visualize="+optVisualize+"]");

	}

	public ObservationModelBuilder setProjectPathAndModelPath(String projectPath, String modelPath){
		this.projectPath = projectPath;
		this.modelPath = modelPath;
		String projectPathTmp = projectPath.replace("\\", "/");
		String[] splitted = projectPathTmp.split("/");
		projectName = splitted[splitted.length-1];
		splitted = modelPath.split("/");
		modelName = splitted[splitted.length-1];
		modelProjectDir = modelPath + "/projects/"+projectName;
		resultDir = modelPath + "/projects/" + projectName+"/results";
		resultFilePath = modelPath + "/projects/" + projectName + "/result.csv";
		checkDir();
		return this;
	}

	public void checkDir(){

		URL projURL = resReader.getResourceURL(projectPath);
		URL modelURL =  resReader.getResourceURL(modelPath);
		URL modelProjectURL =  resReader.getResourceURL(modelProjectDir);

		try{
			if( projURL == null){
				System.out.println("projectPath="+projectPath+" does not exist.");
				throw new IOException();
			}
			if( modelURL == null){
				System.out.println("modelPath="+modelPath + " does not exist.");
				throw new IOException();
			}
			if(modelProjectURL == null){
				if(projURL.getProtocol().equals("file")){
					File file = new File(modelProjectDir);
					file.mkdirs();
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	static final String FORMATPATH = "/format.json";

	public ObservationModelBuilder loadProject(){
	   projData	= loadProject(readsTest);
	   return this;
	}

	private ProjectData loadProject(boolean readsTest) {

		String group = projectName+"-"+modelName;
		ProjectData projData = new ProjectData();

		String formatPath = projectPath+FORMATPATH;
		InputStream is =resReader.getInputStream(formatPath);
		try {
			JSONObject formatJSON = (JSONObject) JSON.parse(is);

			ProjectData.Info projectInfo = loadInformation(formatJSON);
			projData.info(projectInfo);

			MapData mapData = loadMapData(projectPath, formatJSON);
			projData.mapData(mapData);

			TrainData trainData = loadTrainingData(projectPath, formatJSON, mapData, group);
			projData.trainData(trainData);

			if(optVisualize){
				visualizeTrainingData(trainData, projectPath);
			}
			if (readsTest) {
				TestData testData = loadTestData(projectPath, formatJSON, mapData, group);
				projData.testData(testData);
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		this.projData = projData;
		return projData;
	}



	static final String INFORMATION = "information";

	public ProjectData.Info loadInformation(JSONObject formatJSON){

		ProjectData.Info projInfo = null;

		if(formatJSON.has(INFORMATION)){
			try {
				JSONObject info = formatJSON.getJSONObject(INFORMATION);
				projInfo = new ProjectData.Info(info);
				System.out.println("information="+info.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return projInfo;
	}


	static ServiceLoader<TrainDataReader> trainReaderLoader = ServiceLoader.load(TrainDataReader.class);
	static ServiceLoader<TestDataReader> testReaderLoader = ServiceLoader.load(TestDataReader.class);
	static ServiceLoader<MapDataReader> mapReaderLoader = ServiceLoader.load(MapDataReader.class);

	static ServiceLoader<SampleVisualizer> visualizerLoader = ServiceLoader.load(SampleVisualizer.class);

	static public TrainData loadTrainingData(String projectPath, JSONObject formatJSON, MapData mapData, String group){
		for(TrainDataReader reader : trainReaderLoader){
			if(reader.hasReadMethod(formatJSON)){
				System.out.println("TrainDataReader="+reader);
				return reader.read(projectPath, formatJSON, mapData, group);
			}
		}
		throw new RuntimeException("TrainDataReader was not found.");
	}

	public void visualizeTrainingData(TrainData trainData, String projectPath){
		List<Sample> trainSamples = trainData.getSamples();
		for(SampleVisualizer bvis : visualizerLoader){
			bvis.setSamples(trainSamples);
			bvis.visualizeMeanRssi(projectPath);
		}
	}


	static public TestData loadTestData(String projectPath, JSONObject formatJSON, MapData mapData, String group){
		for(TestDataReader reader: testReaderLoader){
			if(reader.hasReadMethod(formatJSON)){
				System.out.println("TestDataReader="+reader);
				return reader.read(projectPath, formatJSON, mapData, group);
			}
		}
		throw new RuntimeException("TestDataReader was not found.");
	}

	static public MapData loadMapData(String projectPath, JSONObject formatJSON){
		MapData mapData = null;
		JSONObject mapFormatJSON = null;
		try {
			mapFormatJSON = formatJSON.getJSONObject("map");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		for(MapDataReader mapReader: mapReaderLoader){
			if(mapReader.hasReadMethod(mapFormatJSON)){
				System.out.println("MapDataReader="+mapReader);
				mapData = mapReader.read(projectPath, mapFormatJSON);
				return mapData;
			}
		}
		throw new RuntimeException("MapDataReader was not found. MapFormatJSON = "+mapFormatJSON);
	}

	public void loadModel(){
		model = loadModel(modelPath, modelProjectDir, optRefresh, projData);

	}

	private ObservationModel loadModel(String modelPath, String modelProjectDir, boolean optRefresh,
			ProjectData projData){

		ObservationModel model = null;

		String modelDefPath = modelPath+"/model.json";
		String trainedModelDef = modelProjectDir + "/model.json";

		URL trainedModelDefURL = resReader.getResourceURL(trainedModelDef);

		boolean useTrainedModel = (!optRefresh) && (trainedModelDefURL!=null);
		if( useTrainedModel ){
			System.out.println("Loading a previously trained model at "+trainedModelDefURL);
			modelDefPath = trainedModelDef;
		}

		System.out.println("Loading "+modelDefPath);
		InputStream is = resReader.getInputStream(modelDefPath);
		JSONObject modelJSON = null;
		try {
			modelJSON = (JSONObject) JSON.parse(is);
			ModelBuilderCore builder = new ModelBuilderCore();
			ObservationModel obsModel = builder.buildModel(modelJSON, projData);
			model = obsModel;
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if(!useTrainedModel){
			try {
				File file = new File(modelDefPath);
				if(file.exists()){
					Writer writer = new FileWriter(trainedModelDef);
					writer.write(modelJSON.toString());
					writer.close();
					System.out.println("Saved a trained model.");
				}else{
					System.out.println("A trained model was not saved.");
				}
				System.out.println("model JSON = "+ modelJSON.toString());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return model;
	}



	// Keys for environment options
	static final String N_THREADS = "nThreads";
	static final String OUTPUT_STATES = "outputStates";

	public void setEnvironmentOption(JSONObject envOptions){
		try{
			if(envOptions!=null){
				if(envOptions.containsKey(N_THREADS)){
					int nThreads = envOptions.getInt(N_THREADS);
					ExecutorServiceHolder.setNThreads(nThreads);
				}
				if(envOptions.containsKey(OUTPUT_STATES)){
					boolean outputStates = envOptions.getBoolean(OUTPUT_STATES);
					LocalizationStatus.setOutputStates(outputStates);
				}
			}
		}
		catch(JSONException e){
			e.printStackTrace();
		}
	}

	public ObservationModelBuilder printStatus(){
		System.out.println("modelPath="+modelPath);
		System.out.println("projectPath="+projectPath);
		return this;
	}

	public void printHelp(){
		System.out.println("");
		System.out.println("usage: <command> -m <path to model> -p <path to project>");
	}


	public LocalizationStatus update(LocalizationStatus locStatus, LocalizationInput locInput){
		LocalizationStatus locStatusNew = model.update(locStatus, locInput);
		return locStatusNew;
	}

	public void destroy(){
		ExecutorServiceHolder.destroy();
	}
}