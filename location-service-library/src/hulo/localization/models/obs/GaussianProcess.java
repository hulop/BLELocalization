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

import hulo.localization.kernels.KernelFunction;
import hulo.localization.likelihood.LikelihoodModel;
import hulo.localization.thread.ExecutorServiceHolder;
import hulo.localization.utils.JSONUtils;
import hulo.localization.utils.StatUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class GaussianProcess {

	int optConstVar = 0;

	LikelihoodModel likelihoodModel = null;

	public void setLikelihoodModel(LikelihoodModel likelihoodModel){
		this.likelihoodModel = likelihoodModel;
	}

	double sigmaN=1;
	double[][] K;
	KernelFunction kernel;
	double[][] X;
	double[][] Y;
	double[][] dY;
	double[][] mX;

	double[][] Ky = null;
	double[][] invKy = null;
	double[][] invKyDY = null;

	double detKy;

	int matrices_are_computed=0;
	double[] Kstar_temp;
	double[] Ks_invKy_temp;

	public GaussianProcess() {}

	public GaussianProcess(GaussianProcess gp){
		this.optConstVar = gp.optConstVar;
		this.likelihoodModel = gp.likelihoodModel;
		this.kernel = gp.kernel;
		this.K = gp.K;
		this.sigmaN = gp.sigmaN;
		this.X = gp.X;
		this.Y = gp.Y;
		this.dY = gp.dY;
		this.mX = gp.mX;
		this.invKyDY = gp.invKyDY;

		this.matrices_are_computed = gp.matrices_are_computed;
		this.Kstar_temp = gp.Kstar_temp;
		this.Ks_invKy_temp = gp.Ks_invKy_temp;
	}


	public JSONObject toJSON(){
		JSONObject jobj = new JSONObject();
		if(invKyDY ==null){
			return jobj;
		}
		JSONArray jsonInvKyDY = JSONUtils.toJSONArray(invKyDY);
		try {
			jobj.put("invKyDY", jsonInvKyDY);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jobj;
	}

	public void fromJSON(JSONObject jobj){
		try {
			JSONArray jsonInvKyDY = jobj.getJSONArray("invKyDY");
			invKyDY = JSONUtils.array2DFromJSONArray(jsonInvKyDY);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void setKernel(KernelFunction kernel){
		this.kernel = kernel;
	}

	public KernelFunction getKernel(){
		return this.kernel;
	}

	public void setSigmaN(double sigmaN){
		this.sigmaN = sigmaN;
	}

	protected double[][] getX(){
		return X;
	}

	protected double[][] getY(){
		return Y;
	}

	protected double[][] getdY(){
		return dY;
	}

	protected double[][] computeGramMatrix(double[][] X){
		int ns = X.length;
		double[][] K = new double[ns][ns];
		for(int i=0; i<ns; i++){
			K[i][i]=kernel.computeKernel(X[i], X[i]);
			for(int j=i+1; j<ns; j++){
				double buff = kernel.computeKernel(X[i], X[j]);
				K[i][j] = buff;
				K[j][i] = buff;
			}
		}
		return K;
	}


	public GaussianProcess fit(double[][] X, double[][] Y){
		int ns = X.length;
		this.X = X;
		this.Y = Y;

		// Compute Gram Matrix (Symmetric matrix)
		K = computeGramMatrix(X);
		RealMatrix KMat = MatrixUtils.createRealMatrix(K);
		RealMatrix sigma_n2I = MatrixUtils.createRealIdentityMatrix(ns).scalarMultiply(sigmaN*sigmaN);
		RealMatrix Ky = KMat.add(sigma_n2I);
		this.Ky = Ky.getData();
		LUDecomposition LUDecomp = new  LUDecomposition(Ky);
		detKy = LUDecomp.getDeterminant();

		RealMatrix invKyMat = LUDecomp.getSolver().getInverse();
		invKy = invKyMat.getData();

		// Precompute dY = Y - mX
		int ny=Y[0].length;
		this.mX = new double[ns][ny];
		this.dY = new double[ns][ny];
		for(int i=0; i<ns; i++){
			for(int j=0; j<ny; j++){
				mX[i][j] = meanFunc(X[i],j);
				dY[i][j] = Y[i][j]-mX[i][j];
			}
		}
		
		if(optConstVar==1){
			invKyDY = computeInvKyDY(invKy, dY);
		}
		
		return this;
	}

	double[][] getInvKyDY(){
		if(invKyDY==null){
			invKyDY = computeInvKyDY(invKy, dY);
		}else{
			if(optConstVar==1){
				return invKyDY;
			}else{
				invKyDY = computeInvKyDY(invKy, dY);
			}
		}
		return this.invKyDY;
	}

	public void setOptConstVar(int optConstVar){
		this.optConstVar = optConstVar;
		System.out.println("optConstVar="+optConstVar+": use a constant variance");
		if(optConstVar==1 && invKy !=null && dY!=null){
			invKyDY = computeInvKyDY(invKy, dY);
		}
	}
	public int getOptConstVar(){
		return optConstVar;
	}

	double[][] computeInvKyDY(double[][] invKy, double[][] dY){
		int ns = dY.length;
		int ny = dY[0].length;
		double[][] invKyDY = new double[ns][ny];

		for(int i=0; i<ns; i++){
			for(int j=0; j<ny; j++){
				invKyDY[i][j] = 0;
				double tmp=0;
				for(int k=0; k<ns; k++){
					tmp += invKy[i][k]*dY[k][j];
				}
				invKyDY[i][j] = tmp;
			}
		}
		return invKyDY;
	}


	public double[][] predictYgivenX(double[][] X){
		int nin = X.length;
		double[][] Y = getY();
		int ny = Y[0].length;
		double[][] Ypred = new double[nin][ny];
		for(int i=0; i<nin; i++){
			Ypred[i] = predictygivenx(X[i]);
		}
		return Ypred;
	}


	protected double meanFunc(double[] x, int index_y){
		return meanFunc(x);
	}

	protected double meanFunc(double[] x){
		return 0;
	}


	public double[] predictygivenx(double[] x){
		return predictfgivenx(x);
	}

	public double[] predictfgivenx(double[] x){
		if(optConstVar==0){
			return predictfgivenxActual(x);
		}else if(optConstVar==1){
			return predictfgivenxConstVar(x);
		}
		return null;
	}

	private void predict_y_var_givenx(double[] x, double[] fpred, double[] variance_y){
		double[][] Y = getY();
		double[][] dY = getdY();

		int ns = Y.length;
		int ny = Y[0].length;

		double[] Kstar = computeKstar(x);
		double[] Ks_invKy = computeKs_invKy(Kstar, invKy);
		double kstar = kernel.computeKernel(x, x);

		double fi = 0;
		for(int i=0; i<ny; i++){
			fi = meanFunc(x,i);
			for(int k=0; k<ns; k++){
				fi += Ks_invKy[k]*dY[k][i];
			}
			fpred[i]=fi;
		}

		double variance_f_i = kstar;
		for(int j=0; j<ns; j++){
			variance_f_i += -Ks_invKy[j]*Kstar[j];
		}

		double sigmaN2 = sigmaN*sigmaN;
		for(int i=0; i<ny; i++){
			variance_y[i]=variance_f_i + sigmaN2;
		}
	}

	private double[] predictfgivenxActual(double[] x){
		double[][] Y = getY();
		double[][] dY = getdY();

		int ns = Y.length;
		double[] Kstar;
		if(matrices_are_computed==1){
			Kstar = Kstar_temp;
		}else{
			Kstar = computeKstar(x);
			Kstar_temp = Kstar;
		}

		int ny = Y[0].length;

		double[] Ks_invKpSig;

		if(matrices_are_computed==1){
			Ks_invKpSig = Ks_invKy_temp;
		}else{
			Ks_invKpSig = computeKs_invKy(Kstar, invKy);
			Ks_invKy_temp = Ks_invKpSig;
		}

		double[] fpred= new double[ny];
		double fi = 0;
		for(int i=0; i<ny; i++){
			fi = meanFunc(x,i);
			for(int k=0; k<ns; k++){
				fi+=Ks_invKpSig[k]*dY[k][i];
			}
			fpred[i]=fi;
		}

		return fpred;
	}

	private double[] predictfgivenxConstVar(double[] x){
		double[][] Y = getY();
		double[][] invKyDY = getInvKyDY();
		int ns = Y.length;
		double[] Kstar = computeKstar(x);

		int ny = Y[0].length;

		double[] fpred= new double[ny];
		double fi = 0;
		for(int i=0; i<ny; i++){
			fi = meanFunc(x,i);
			for(int k=0; k<ns; k++){
				fi+=Kstar[k]*invKyDY[k][i];
			}
			fpred[i]=fi;
		}
		return fpred;
	}

	private double[] computeKstar(double[] x){
		int ns = X.length;
		double[] Kstar = new double[ns];
		for(int i=0; i<ns; i++){
			Kstar[i] = kernel.computeKernel(x, X[i]);
		}
		return Kstar;
	}

	private double[] computeKs_invKy(double[] Kstar, double[][] invKy){
		int ns = Kstar.length;
		double[] Ks_invKy = new double[ns];
		for(int j=0; j<ns; j++){
			Ks_invKy[j]=0.0;
			for(int k=0; k<ns; k++){
				double buff = Kstar[k]*invKy[k][j];
				Ks_invKy[j] += buff;
			}
		}
		return Ks_invKy;
	}

	protected double[] variancefgivenx(double[] x){
		if(optConstVar==0){
			return variancefgivenxActual(x);
		}else if(optConstVar==1){
			return variancefgivenxConstVar(x);
		}
		return null;
	}

	private double[] variancefgivenxActual(double[] x){
		double[][] Y = getY();

		int ns = Y.length;
		int ny = Y[0].length;

		double kstar = kernel.computeKernel(x, x);

		double[] Kstar;
		if(matrices_are_computed==1){
			Kstar = Kstar_temp;
		}else{
			Kstar = computeKstar(x);
		}

		double[] Ks_invKy;
		if(matrices_are_computed==1){
			Ks_invKy  = Ks_invKy_temp;
		}else{
			Ks_invKy = computeKs_invKy(Kstar, invKy);
		}

		double variance_f_i = kstar;
		for(int j=0; j<ns; j++){
			variance_f_i += -Ks_invKy[j]*Kstar[j];
		}

		double[] variance_f= new double[ny];
		for(int i=0; i<ny; i++){
			variance_f[i]=variance_f_i;
		}

		Kstar_temp = Kstar;
		Ks_invKy_temp = Ks_invKy;
		return variance_f;
	}

	private double[] variancefgivenxConstVar(double[] x){
		double[][] Y = getY();
		int ny = Y[0].length;
		double kstar = kernel.computeKernel(x, x);
		double[] variance_f= new double[ny];
		for(int i=0; i<ny; i++){
			variance_f[i]=kstar;
		}
		return variance_f;
	}

	public double[] varianceygivenx(double[] x){
		double[] varfgiveny = variancefgivenx(x);
		int ny = varfgiveny.length;
		double varN = sigmaN*sigmaN;
		for(int i=0; i<ny; i++){
			varfgiveny[i]+=varN;
		}
		return varfgiveny;
	}

	public double logProbaygivenx(double[]x, double[] y){

		double[] mean_y = new double[y.length];
		double[] var_y = new double[y.length];

		if(optConstVar==0){
			predict_y_var_givenx(x, mean_y, var_y);
		}if(optConstVar==1){
			mean_y = predictfgivenx(x);
			var_y = varianceygivenx(x);
		}

		int ny = mean_y.length;
		double sumLogLL=0;
		double sigma = 0;
		for(int j=0; j<ny; j++){
			sigma = Math.sqrt(var_y[j]);

			sumLogLL += logProba(y[j], mean_y[j], sigma);
		}
		return sumLogLL;
	}

	private double logProba(double y, double mean_y, double sigma){
		if(likelihoodModel==null){
			return StatUtils.logProbaNormal(y, mean_y, sigma);
		}else{
			return likelihoodModel.logProba(y, mean_y, sigma);
		}
	}

	public double[] logProbaygivenX(double[][]X, double[] y){
		return logProbaygivenXMultiThread(X, y);
	}

	public double[] logProbaygivenXSingleThread(double[][]X, double[] y){
		int n = X.length;
		final double logpro[] = new double[n];
		for(int i=0; i<n; i++){
			logpro[i]=logProbaygivenx(X[i], y);
		}
		return logpro;
	}

	public double[] logProbaygivenXMultiThread(final double[][]X, final double[] y){
		ExecutorService ex = ExecutorServiceHolder.getExecutorService();

		int n = X.length;
		final double logpro[] = new double[n];
		Future<?>[] futures = new Future<?>[n];
		for(int i=0; i<n; i++){
			final int idx = i;
			futures[i] = ex.submit(new Runnable() {
				public void run() {
					logpro[idx]=logProbaygivenx(X[idx], y);
				}
			});
		}

		for(int i=0; i < n; i++) {
			try {
				futures[i].get();
			} catch (InterruptedException |ExecutionException e) {
				e.printStackTrace();
			}
		}
		return logpro;
	}

	public double looMSE(){

		double[][] Y = getY();
		double[][] dY = getdY();

		int ns = X.length;
		int ny = Y[0].length;
		RealMatrix invKy = MatrixUtils.createRealMatrix(this.invKy);
		RealMatrix K = MatrixUtils.createRealMatrix(this.K);
		RealMatrix Hmat = invKy.multiply(K);
		double[][] H = Hmat.getData();

		double sum=0;
		double count=0;
		for(int j=0;j<ny; j++){
			for(int i=0; i<ns; i++){
				double preddY=0;
				for(int k=0; k<ns; k++){
					preddY += H[i][k]*dY[k][j];
				}
				double diff = (dY[i][j] - preddY)/(1.0 - H[i][i]);
				sum += (diff*diff);
				count += 1;
			}
		}
		sum/=count;
		return sum;
	}

	public double looPredLogLikelihood(){

		double[][] Y = getY();
		double[][] dY = getdY();

		int ns = X.length;
		int ny = Y[0].length;

		RealMatrix Ky = MatrixUtils.createRealMatrix(this.Ky);
		RealMatrix invKy = new LUDecomposition(Ky).getSolver().getInverse();

		RealMatrix dYmat = MatrixUtils.createRealMatrix(dY);

		double[] LOOPredLL = new double[ny];
		for(int j=0;j<ny; j++){
			RealMatrix dy = MatrixUtils.createColumnRealMatrix(dYmat.getColumn(j));
			RealMatrix invKdy = invKy.multiply(dy);
			double sum=0;
			for(int i=0; i<ns; i++){
				double mu_i = dYmat.getEntry(i, j) - invKdy.getEntry(i, 0)/invKy.getEntry(i, i);
				double sigma_i2 = 1/invKy.getEntry(i, i);
				double logLL = StatUtils.logProbaNormal(dYmat.getEntry(i, j), mu_i, Math.sqrt(sigma_i2));
				sum+=logLL;
			}
			LOOPredLL[j] = sum;
		}

		double sumLOOPredLL=0;
		for(int j=0;j<ny; j++){
			sumLOOPredLL += LOOPredLL[j];
		}

		return sumLOOPredLL;
	}

}
