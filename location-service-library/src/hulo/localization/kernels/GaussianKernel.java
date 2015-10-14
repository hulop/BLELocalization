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

package hulo.localization.kernels;

public class GaussianKernel implements KernelFunction{
	double stdev=1;
	double length=1;

	public void setStdev(double stdev){
		this.stdev=stdev;
	}
	public void setLength(double length){
		this.length=length;
	}

	double lengthes[] = null;
	public void setLengthes(double lengthes[]){
		this.lengthes=lengthes;
	}

	public double computeKernel(double[] x1, double[] x2){
		if(lengthes==null){
			setUpLengthes(x1.length);
		}
		int nx=x1.length;
		assert(nx==x2.length);
		double posiInExp=0;
		for(int i=0; i<nx; i++){
			double lg = lengthes[i];
			double diff = x1[i]-x2[i];
			double square = diff*diff;
			posiInExp += square/(2.0*lg*lg);
		}
		return stdev*stdev*Math.exp(-posiInExp);
	}

	private void setUpLengthes(int n){
		lengthes = new double[n];
		for(int i=0; i<n; i++){
			lengthes[i]=length;
		}
	}

}