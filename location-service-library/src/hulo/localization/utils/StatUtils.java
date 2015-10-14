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


public class StatUtils {

	public static double probaNormal(double x, double mu, double sigma){
		return 1.0/(Math.sqrt(2.0*Math.PI)*sigma)*Math.exp(-Math.pow(x-mu, 2)/(2*sigma*sigma));
	}

	public static double logProbaNormal(double x, double mu, double sigma){
		return -1.0/2.0*Math.log(2*Math.PI) -1.0/2.0*Math.log(sigma*sigma)
				-Math.pow(x-mu, 2)/(2.0*sigma*sigma);
	}

	public static double probaNormalContami(double x, double mu, double sigma, double coeff,  double contamiRate){
		double p1 = probaNormal(x, mu, sigma);
		double p2 = probaNormal(x, mu, Math.sqrt(coeff)*sigma);
		double p = (1-contamiRate)*p1 + contamiRate*p2;
		return p;
	}

	public static double logProbaNormalContami(double x, double mu, double sigma, double coeff,  double contamiRate){
		double p = probaNormalContami(x, mu, sigma, coeff, contamiRate);
		return Math.log(p);
	}

	public static double coeffDetermination(double[] y, double[] ypred){
		double ymean = ArrayUtils.mean(y);
		double deno = 0;
		double nume = 0;
		for(int i=0; i<y.length; i++){
			nume += (y[i]-ypred[i])*(y[i]-ypred[i]);
			deno += (y[i]-ymean)*(y[i]-ymean);
		}
		double r = 1.0 - nume/deno;
		return r;
	}

}
