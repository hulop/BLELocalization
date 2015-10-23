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
import hulo.localization.utils.StatUtils;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;


public class GaussianProcessLDPLMean extends GaussianProcess {

	// LDPL Data
	final int NDIM=3;
	final double minDist = 0.01;
	final double maxRssi = Beacon.maxRssi;
	final double minRssi = Beacon.minRssi;
	double[] params = {2.0, -70.0, 4.0, 0.0};// n, A, floor_a, floor_b

	int floorDiffLimit = 1000;
	double threshFloorDiff = 1e-6;

	double hDiff = 0.0;
	public void setHDiff(double hDiff){
		this.hDiff = hDiff;
	}

	double[][] sourceLocs = null;
	int[] activeBeaconList = null;

	double[][] activeSourceLocs = null;
	double[][] activeY;
	double[][] activeDY;

	double[][] mask = null;
	double[][] activeMask = null;
	boolean useMask = true;

	public GaussianProcessLDPLMean(){}

	public GaussianProcessLDPLMean(GaussianProcessLDPLMean gp){
		super(gp);
		// Parameters in GaussianProcessLDPLMean
		this.params = gp.params;
		this.hDiff = gp.hDiff;
		this.sourceLocs = gp.sourceLocs;
		this.mask = gp.mask;
		this.useMask = gp.useMask;
	}


	public void setParams(double[] params){
		this.params=params;
	}

	public double[] getParams(){
		return params;
	}

	public void setSourceLocs(double[][] sourceLocs){
		this.sourceLocs = sourceLocs;
	}

	public void setUseMask(boolean useMask){
		this.useMask = useMask;
		System.out.println("GaussianProcessLDPLMean.useMask="+this.useMask);
	}
	public boolean getUseMask(){
		return useMask;
	}

	double[][] getActiveSoureceLocs(){
		return activeSourceLocs;
	}

	public void updateByActiveBeaconList(int[] activeBeaconList){
		setActiveBeaconList(activeBeaconList);
		setUpActiveYandActiveDY(activeBeaconList);
	}

	public void setActiveBeaconList(int[] activeBeaconList){
		this.activeBeaconList = activeBeaconList;
		setUpActiveSourceLocs(activeBeaconList);
	}


	double[][] activeInvKyDY;

	@Override
	double[][] getInvKyDY(){
		return this.activeInvKyDY;
	}

	protected void setUpActiveYandActiveDY(int[] activeBeaconList){
		int ns = Y.length;
		int ny = activeBeaconList.length;

		double[][] activeY = new double[ns][ny];
		double[][] activeDY = new double[ns][ny];
		double[][] activeMask = new double[ns][ny];

		if(optConstVar==1){
			activeInvKyDY = new double[ns][ny];
		}

		for(int i=0; i<ns; i++){
			for(int j=0; j<ny; j++){
				int id = activeBeaconList[j];
				activeY[i][j] = Y[i][id];
				activeDY[i][j] = dY[i][id];
				activeMask[i][j] = mask[i][id];
				if(optConstVar==1){
					activeInvKyDY[i][j] = super.getInvKyDY()[i][id];
				}
			}
		}

		this.activeY=activeY;
		this.activeDY=activeDY;
		this.activeMask = activeMask;
	}

	protected void setUpActiveSourceLocs(int[] activeBeaconList){
		int ny = activeBeaconList.length;
		double[][] activeSourceLocs = new double[ny][NDIM];
		for(int j=0; j<ny; j++){
			int id = activeBeaconList[j];
			activeSourceLocs[j] = sourceLocs[id];
		}
		this.activeSourceLocs = activeSourceLocs;
	}

	@Override
	protected double[][] getY(){
		return activeY;
	}

	@Override
	protected double[][] getdY(){
		return activeDY;
	}

	protected double[][] getMask(){
		return activeMask;
	}

	double[][] getActiveSourceLocs(){
		return activeSourceLocs;
	}

	double distance(double[] x1, double[] x2){
		return distance3D(x1, x2);
	}

	double distance2D(double[] x1, double[] x2){
		double dx = x1[0]-x2[0];
		double dy = x1[1]-x2[1];
		double d = Math.sqrt(dx*dx + dy*dy);
		return minDist<d ? d : minDist;
	}

	double distance3D(double[] x1, double[] x2){
		double dx = x1[0]-x2[0];
		double dy = x1[1]-x2[1];
		double dh = hDiff;
		double d = Math.sqrt(dx*dx + dy*dy + dh*dh);
		return minDist<d ? d : minDist;
	}

	double floorDiff(double[] x1, double[] x2){
		double fdiff = Math.abs(x1[2]-x2[2]);
		if(floorDiffLimit<fdiff){ // Special cases
			return 0;
		}
		return fdiff;
	}

	double ITUModel(double[] xr, double[] xs){
		return -10.0*params[0]*Math.log10(distance(xr, xs)) + params[1]
				- lossByFloorDiff(floorDiff(xr,xs));
	}


	double lossByFloorDiff(double floorDiff){
		if(floorDiff<(1-threshFloorDiff)){
			return 0;
		}else{
			return params[2]*floorDiff + params[3];
		}
	}

	@Override
	protected double meanFunc(double x[], int index_y){
		double[] xs = getActiveSourceLocs()[index_y];
		double rssi = ITUModel(x, xs);
		return minRssi<rssi ? rssi : minRssi;
	}

	@Override
	public GaussianProcessLDPLMean fit(double[][] X , double[][] Y){
		int ns = X.length;
		int ny = Y[0].length;

		int[] actBeaList = new int[ny];
		for(int i=0; i<ny; i++){
			actBeaList[i]=i;
		}
		this.setActiveBeaconList(actBeaList);

		super.fit(X, Y);

		mask = new double[ns][ny];
		for(int i=0; i<ns; i++){
			for(int j=0; j<ny; j++){
				mask[i][j] = 1;
				if(useMask && Y[i][j]==minRssi){
					mask[i][j] = 0;
				}
			}
		}
		updateByActiveBeaconList(actBeaList);
		return this;
	}

	@Override
	public double looMSE(){

		double[][] Y = getY();
		double[][] dY = getdY();
		double[][] mask = getMask();

		int ns = X.length;
		int ny = Y[0].length;
		RealMatrix invKy = MatrixUtils.createRealMatrix(this.invKy);
		RealMatrix K = MatrixUtils.createRealMatrix(this.K);
		RealMatrix Hmat = invKy.multiply(K);
		double[][] H = Hmat.getData(); // H = (K+sI)^(-1) K = invKy K

		double sum=0;
		double count=0;
		for(int j=0;j<ny; j++){
			for(int i=0; i<ns; i++){
				if(mask[i][j]>0.0){
					double preddY=0;
					for(int k=0; k<ns; k++){
						preddY += H[i][k]*dY[k][j];
					}
					double diff = (dY[i][j] - preddY)/(1.0 - H[i][i]);
					sum += (diff*diff) * mask[i][j];
					count += mask[i][j];
				}
			}
		}
		sum/=count;
		return sum;
	}

	@Override
	public double looPredLogLikelihood(){

		double[][] Y = getY();
		double[][] dY = getdY();
		double[][] mask = getMask();

		int ns = Y.length;
		int ny = Y[0].length;

		RealMatrix Ky = MatrixUtils.createRealMatrix(this.Ky);
		RealMatrix invK = new LUDecomposition(Ky).getSolver().getInverse();

		RealMatrix dYmat = MatrixUtils.createRealMatrix(dY);

		double[] LOOPredLL = new double[ny];
		for(int j=0;j<ny; j++){
			RealMatrix dy = MatrixUtils.createColumnRealMatrix(dYmat.getColumn(j));
			RealMatrix invKdy = invK.multiply(dy);
			double sum=0;
			for(int i=0; i<ns; i++){
				double mu_i = dYmat.getEntry(i, j) - invKdy.getEntry(i, 0)/invK.getEntry(i, i);
				double sigma_i2 = 1/invK.getEntry(i, i);
				double logLL = StatUtils.logProbaNormal(dYmat.getEntry(i, j), mu_i, Math.sqrt(sigma_i2));
				sum += logLL * mask[i][j];
			}
			LOOPredLL[j] = sum;
		}

		double sumLOOPredLL=0;
		for(int j=0;j<ny; j++){
			sumLOOPredLL += LOOPredLL[j];
		}

		return sumLOOPredLL;
	}

	static MaxEval maxEval = new MaxEval(10000);
	static PointValuePair minimize(MultivariateFunction func, double[] pointInit, double[] diffPointInit){
		SimplexOptimizer optimizer = new SimplexOptimizer(1e-5, 1e-10);
		NelderMeadSimplex simplex = new NelderMeadSimplex(diffPointInit);
		ObjectiveFunction objFunc = new ObjectiveFunction(func);
		InitialGuess initGuess = new InitialGuess(pointInit);
		PointValuePair pair = optimizer.optimize(maxEval, objFunc, GoalType.MINIMIZE, initGuess, simplex);
		return pair;
	}

}
