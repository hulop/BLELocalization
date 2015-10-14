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

import hulo.localization.utils.ArrayUtils;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.PointValuePair;


public class GaussianProcessLDPLMeanMulti extends GaussianProcessLDPLMean {

	final int PDIM=4;
	final double threshFloorDiff = 1e-9;

	double[] lambdas = {1000.0, 0.001, 1.0, 1.0};


	double[][] paramsArray = null;

	public GaussianProcessLDPLMeanMulti(){}

	public GaussianProcessLDPLMeanMulti(GaussianProcessLDPLMeanMulti gp){
		super(gp);
		this.lambdas = gp.lambdas;
		this.paramsArray = gp.paramsArray;
	}

	protected void copyParameters(GaussianProcessLDPLMeanMulti gp){
		this.lambdas = gp.lambdas;
		this.paramsArray = gp.paramsArray;
	}

	public void setStabilizeParameter(double[] lambdas){
		this.lambdas = lambdas;
	}

	public double[] getStabilizeParameter(){
		return lambdas;
	}

	public double[][] getParamsArray(){
		return this.paramsArray;
	}

	public void setParamsArray(double[][] paramsArray){
		this.paramsArray = paramsArray;
	}

	double ITUModel(double[] xr, int id){
		double[] xs = sourceLocs[id];
		return -10.0*paramsArray[id][0]*Math.log10(distance(xr, xs)) + paramsArray[id][1]
				- lossByFloorDiff(floorDiff(xr,xs), id);
	}

	double lossByFloorDiff(double floorDiff, int id){
		if(floorDiff<threshFloorDiff){
			return 0;
		}else{
			return paramsArray[id][2]*floorDiff + paramsArray[id][3];
		}
	}

	@Override
	protected double meanFunc(double x[], int index_y){
		int id = activeBeaconList[index_y];
		double rssi = ITUModel(x, id);
		return minRssi<rssi ? rssi : minRssi;
	}

	protected double[] transformFeature(double[] xr, int index_y){
		int id = activeBeaconList[index_y];
		double[] xs = sourceLocs[id];

		double[] feature = new double[PDIM];
		feature[0] = -10.0*Math.log10(distance(xr, xs));
		feature[1] = 1;

		double eps = 1e-6;
		feature[2] = floorDiff(xr, xs);
		feature[3] = feature[2]>eps ? 1.0 : 0.0;

		return feature;
	}

	@Override
	public GaussianProcessLDPLMeanMulti fit(double[][] X, double[][] Y){
		int ny=Y[0].length;
		if(paramsArray==null){
			paramsArray = new double[ny][PDIM];
			for(int i=0; i<ny; i++){
				paramsArray[i] = params.clone();
			}
		}
		super.fit(X, Y);
		return this;
	}

	public double looMSEwithStabilize(){
		return looMSE() + stabilizationTerm();
	}

	public double stabilizationTerm(){
		double[] paramsMean = ArrayUtils.mean(paramsArray);

		int np = params.length;
		int ny = paramsArray.length;

		double regu = 0;
		for(int j=0; j<np; j++){
			double regu_j = 0;
			for(int i=0; i< ny; i++){
				double diff = paramsArray[i][j] - paramsMean[j];
				regu_j += diff*diff;
			}
			regu_j *= lambdas[j]*1.0/2.0;
			regu += regu_j;
		}

		return regu;
	}

	public double looErrorLDPLMultiPart(int k){
		looUpdateLDPLMulti(k);
		int ns = Y.length;
		int ny = Y[0].length;
		int count=0;
		double sum = 0;;
		for(int j=0; j<ny; j++){
			if(Y[k][j]>minRssi){
				double[] feat = transformFeature(X[k], j);
				double ypred = 0;
				for(int i=0; i<feat.length; i++){
					ypred += feat[i]*paramsArray[j][i];
				}
				double diff = Y[k][j] - ypred;
				sum += diff*diff;
				count+=1;
			}
		}
		if(count!=0){
			sum/=count;
		}
		return sum;
	}


	void looUpdateLDPLMulti(Integer i_lvd){
		double[] paramsMean = params;

		int ns = Y.length;
		int ny = Y[0].length;
		for(int j=0; j<ny; j++){
			double[][] A = new double[2][2];
			double[] b = new double[2];

			A[0][0] = lambdas[0];
			A[1][1] = lambdas[1];
			A[0][1] =0;
			A[1][0]=0;
			b[0] = lambdas[0]*paramsMean[0];
			b[1] = lambdas[1]*paramsMean[1];

			for(int i=0; i<ns; i++){
				if(Integer.valueOf(i)!=i_lvd){
					if(Y[i][j]>minRssi){
						double[] feat = transformFeature(X[i], j);
						A[0][0]+=feat[0]*feat[0];
						A[0][1]+=feat[0];
						A[1][0]+=feat[0];
						A[1][1]+=feat[1];
						b[0] += feat[0] * Y[i][j];
						b[1] += Y[i][j];
					}
				}
			}

			double det = A[0][0]*A[1][1] - A[0][1]*A[1][0];
			double[][] invA = {{A[1][1]/det, -A[0][1]/det},{ -A[1][0]/det, A[0][0]/det }};

			double Aj = invA[0][0]*b[0]+invA[0][1]*b[1];
			double bj = invA[1][0]*b[0]+invA[1][1]*b[1];

			paramsArray[j][0] = Aj;
			paramsArray[j][1] = bj;
		}

	}

	void looUpdateLDPLMultiFloorDiff(Integer i_lvd){
		double[] paramsMean = params;

		int ns = Y.length;
		int ny = Y[0].length;
		for(int j=0; j<ny; j++){
			double[][] A = new double[2][2];
			double[] b = new double[2];

			A[0][0] = lambdas[2];
			A[1][1] = lambdas[3];
			A[0][1] = 0;
			A[1][0] = 0;
			b[0] = lambdas[2]*paramsMean[2];
			b[1] = lambdas[3]*paramsMean[3];

			for(int i=0; i<ns; i++){
				if(Integer.valueOf(i)!=i_lvd){
					if(Y[i][j]>minRssi){
						double[] feat = transformFeature(X[i], j);
						A[0][0]+=feat[2]*feat[2];
						A[0][1]+=feat[2];
						A[1][0]+=feat[2];
						A[1][1]+=feat[3];
						b[0] += feat[2] * Y[i][j];
						b[1] += Y[i][j];
					}
				}
			}

			double det = A[0][0]*A[1][1] - A[0][1]*A[1][0];
			double[][] invA = {{A[1][1]/det, -A[0][1]/det},{ -A[1][0]/det, A[0][0]/det }};

			double Aj = invA[0][0]*b[0]+invA[0][1]*b[1];
			double bj = invA[1][0]*b[0]+invA[1][1]*b[1];

			paramsArray[j][2] = Aj;
			paramsArray[j][3] = bj;
		}
	}

	public void optimizeLDPLMultiHyperParams(){

		final GaussianProcessLDPLMeanMulti gpLDPLMulti = this;

		MultivariateFunction costFunc = new MultivariateFunction(){
			@Override
			public double value(double[] point){

				double[] params = gpLDPLMulti.getParams();
				params[0] = point[0];
				params[1] = point[1];
				gpLDPLMulti.setParams(params);
				double[] lmdRegu = gpLDPLMulti.getStabilizeParameter();
				lmdRegu[0] = Math.pow(10, point[2]);
				lmdRegu[1] = Math.pow(10, point[3]);
				gpLDPLMulti.setStabilizeParameter(lmdRegu);

				int ns = X.length;
				double aveLOOMSE = 0.0;
				for(int k=0; k<ns; k++){
					double looMSE = gpLDPLMulti.fit(X, Y).looErrorLDPLMultiPart(k);
					aveLOOMSE += looMSE/ns;
				}

				final StringBuilder sb = new StringBuilder();
				sb.setLength(0);
				sb.append("optimizeLDPLMultiHyperParams: ");
				sb.append("aveLOOMSE="+aveLOOMSE+", ");
				sb.append("n="+params[0]+",A="+params[1]+", lmdn="+lmdRegu[0]+",lmdA="+lmdRegu[1]);
				sb.append(", ns="+ns);
				System.out.println(sb.toString());

				return aveLOOMSE;
			}
		};

		double[] pointInit = { params[0], params[1], Math.log10(lambdas[0]), Math.log10(lambdas[1])};
		double[] dPointInit = {0.1, 1.0, 0.1, 0.1};

		PointValuePair pair = GaussianProcessLDPLMean.minimize(costFunc, pointInit, dPointInit);
		double[] point = pair.getPoint();
		double[] params = gpLDPLMulti.getParams();
		params[0] = point[0];
		params[1] = point[1];
		gpLDPLMulti.setParams(params);
		double[] lambdas = gpLDPLMulti.getStabilizeParameter();
		lambdas[0] = Math.pow(10, point[2]);
		lambdas[1] = Math.pow(10, point[3]);
		gpLDPLMulti.setStabilizeParameter(lambdas);

	}

	public void optimizeLDPLMultiParameters(){
		looUpdateLDPLMulti(null);
		System.out.println(toStringParamsArray());
	}

	public String toStringParamsArray(){
		final StringBuilder sb = new StringBuilder();
		sb.setLength(0);
		sb.append("LDPLMulti parameters: ");
		for(int j=0; j<paramsArray.length; j++){
			double Aj=paramsArray[j][0];
			double bj=paramsArray[j][1];
			double fa = paramsArray[j][2];
			double fb = paramsArray[j][3];
			sb.append("reassignedId="+j+", A="+Aj+", b="+bj+",fa="+fa+",fb="+fb+", ");
		}
		sb.append(System.getProperty("line.separator"));

		sb.append("LDPLMulti parameters (statistics): ");

		double[] paramsMean = ArrayUtils.mean(paramsArray);
		double[] paramsVar = ArrayUtils.var(paramsArray);
		double[] paramsMin = ArrayUtils.min(paramsArray);
		double[] paramsMax = ArrayUtils.max(paramsArray);

		sb.append("mean: A="+paramsMean[0]+", b="+paramsMean[1]+",fa="+paramsMean[2]+",fb="+paramsMean[3]);
		sb.append(",");
		sb.append("variance: A="+paramsVar[0]+", b="+paramsVar[1]+",fa="+paramsVar[2]+",fb="+paramsVar[3]);
		sb.append(",");
		sb.append("min: A="+paramsMin[0]+", b="+paramsMin[1]+",fa="+paramsMin[2]+",fb="+paramsMin[3]);
		sb.append(",");
		sb.append("max: A="+paramsMax[0]+", b="+paramsMax[1]+",fa="+paramsMax[2]+",fb="+paramsMax[3]);
		return sb.toString();
	}

}
