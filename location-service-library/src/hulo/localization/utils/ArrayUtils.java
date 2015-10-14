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

package hulo.localization.utils;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;



public class ArrayUtils {

	public static int[] toIntArray(Integer[] array){
		int n= array.length;
		int[] intArray = new int[n];
		for(int i=0; i<n;i++){
			intArray[i] = array[i];
		}
		return intArray;
	}

	public static Integer[] toIntegerArray(int[] intArray){
		int n = intArray.length;
		Integer[] IntegerArray = new Integer[n];
		for(int i=0; i<n; i++){
			IntegerArray[i]=intArray[i];
		}
		return IntegerArray;
	}


	public static double[] column(double[][] array, int j){
		int rows = array.length;
		double[] colvec = new double[rows];
		for(int i=0; i<rows; i++){
			colvec[i] = array[i][j];
		}
		return colvec;
	}

	public static double[] row(double[][] array, int i){
		return array[i];
	}

	public static double min(double[] a){
		double min = a[0];
		for(int i=0;i<a.length; i++){
			min = Math.min(min, a[i]);
		}
		return min;
	}

	public static double max(double[] a){
		double max = a[0];
		for(int i=0;i<a.length; i++){
			max = Math.max(max, a[i]);
		}
		return max;
	}

	public static String arrayToString(double[] array){
		StringBuilder sb = new StringBuilder();
		int n=array.length;
		for(int i=0; i<n ;i++){
			if(0<i) sb.append(",");
			sb.append(array[i]);
		}
		return sb.toString();
	}

	public static String arrayToString(double[][] array){
		StringBuilder sb = new StringBuilder();
		int n=array.length;
		for(int i=0; i<n ;i++){
			if(0<i) sb.append("\n");
			sb.append(arrayToString(array[i]));
		}
		return sb.toString();
	}

	public static double[] toDouble(float[] array){
		int n = array.length;
		double[] a2 = new double[n];
		for(int i=0;i<n; i++){
			a2[i] = array[i];
		}
		return a2;
	}

	public static float mean(float[] array){
		return (float) mean(toDouble(array));
	}

	public static double sum(double[] x){
		int n= x.length;
		double s = 0;
		for(int i=0; i<n; i++){
			s += x[i];
		}
		return s;
	}

	public static double[] sum(double[][] X , int axis){
		if(axis==0){
			return sumAxis0(X);
		}else if(axis==1){
			return sumAxis1(X);
		}else{
			String message = "axis>=2 is not supported in sum method.";
			throw new RuntimeException(message);
		}
	}

	static double[] sumAxis0(double[][] X){
		int m = X[0].length;
		double[] array = new double[m];
		for(int j=0; j<m; j++){
			double[] x = column(X, j);
			array[j] = sum(x);
		}
		return array;
	}

	static double[] sumAxis1(double[][] X){
		int n = X.length;
		double[] array = new double[n];
		for(int i=0; i<n; i++){
			double[] x = row(X, i);
			array[i] = sum(x);
		}
		return array;
	}

	public static double[] min(double[][] array){
		int np = array[0].length;
		double[] minArray = new double[np];
		for(int j=0; j<np ;j++){
			double[] column = ArrayUtils.column(array, j);
			minArray [j] = ArrayUtils.min(column);
		}
		return minArray ;
	}

	public static double[] max(double[][] array){
		int np = array[0].length;
		double[] maxArray = new double[np];
		for(int j=0; j<np ;j++){
			double[] column = ArrayUtils.column(array, j);
			maxArray[j] = ArrayUtils.max(column);
		}
		return maxArray;
	}


	public static double mean(double[] array){
		int n=array.length;
		double sum = 0;

		for(int i=0; i<n; i++){
			sum+=array[i];
		}
		double mean = sum/((double)n);
		return mean;
	}

	public static double[] mean(double[][] array){
		int n=array.length;
		int m = array[0].length;

		double[] mean = new double[m];

		for(int i=0; i<n; i++){
			for(int j=0; j<m; j++){
				mean[j] += array[i][j];
			}
		}
		for(int j=0; j<m; j++){
			mean[j]/=n;
		}
		return mean;
	}

	public static double var(double[] array){
		double mean = mean(array);
		int n = array.length;
		double var = 0;
		for(int i=0; i<n; i++){
			double val = array[i] - mean;
			var += val*val;
		}
		return var/n;
	}


	public static double[] var(double[][] array){
		double[] mean = mean(array);

		int n=array.length;
		int m = array[0].length;

		double[] var = new double[m];
		for(int i=0; i<n; i++){
			for(int j=0; j<m; j++){
				double val = array[i][j] -mean[j];
				var[j] += val*val;
			}
		}
		for(int j=0; j<m; j++){
			var[j]/=n;
		}
		return var;
	}

	public static double stdev(double[] array){
		double var = var(array);
		return Math.sqrt(var);
	}

	public static double[] stdev(double[][] array){
		double[] var = var(array);
		for(int i=0; i<var.length; i++){
			var[i] = Math.sqrt(var[i]);
		}
		return var;
	}

	public static double[] addScalar(double[] array, double scalar){
		int n = array.length;
		double[] arrayNew = new double[n];
		for(int i=0; i<n; i++){
			arrayNew[i] = array[i] + scalar;
		}
		return arrayNew;
	}

	public static double[] multiply(double[][] X1, double[] x2){
		RealMatrix M1 = MatrixUtils.createRealMatrix(X1);
		return M1.operate(x2);
	}


	public static double[][] multiply(double[][] X1, double[][] X2){
		RealMatrix M1 = MatrixUtils.createRealMatrix(X1);
		RealMatrix M2 = MatrixUtils.createRealMatrix(X2);
		RealMatrix M3 = M1.multiply(M2);
		return M3.getData();
	}

	public static double[][] normalize(double[][] X){
		int n = X.length;
		int m = X[0].length;

		double eps = 1e-12;

		double[] mean = mean(X);
		double[] stds = stdev(X);
		double[][] Xnew = new double[n][m];

		for(int i=0; i<n; i++){
			for(int j=0; j<m; j++){
				Xnew[i][j] = (X[i][j] - mean[j])/(stds[j]+eps);
			}
		}
		return Xnew;
	}

	public static double[] flatten(double[][] Y){
		int n=Y.length;
		int m=Y[0].length;
		double[] y = new double[n*m];

		for(int i=0; i<n; i++){
			for(int j=0; j<m; j++){
				y[i*m+j] = Y[i][j];
			}
		}
		return y;
	}

	public static double[][] inverseMat(double[][] mat){
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(mat);
		LUDecomposition lu = new LUDecomposition(realMatrix);
		RealMatrix invMat = lu.getSolver().getInverse();
		return invMat.getData();
	}

}
