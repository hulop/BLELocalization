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

package hulo.localization.pf;

import hulo.localization.State;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GridResampler implements Resampler {

	static final String SYSTEMATIC_RESAMPLING = "systematicResampling";
	static final String STRATIFIED_RESAMPLING = "stratifiedResampling";
	enum GridType{stratified, systematic};

	Random rand = new Random();
	String resamplingMethod;
	StringBuilder sb = new StringBuilder();

	@Override
	public boolean hasResamplingMethod(String resamplingMethod){
		this.resamplingMethod = resamplingMethod;
		if(resamplingMethod.equals(SYSTEMATIC_RESAMPLING)){
			return true;
		}else if (resamplingMethod.equals(STRATIFIED_RESAMPLING)){
			return true;
		}
		return false;
	}

	@Override
	public State[] resampling(State[] states, double[] weights) {
		GridType gType = GridType.systematic;
		if(resamplingMethod.equals(SYSTEMATIC_RESAMPLING)){
			 gType = GridType.systematic;
		}else if (resamplingMethod.equals(STRATIFIED_RESAMPLING)){
			 gType = GridType.stratified;
		}
		List<State> statesNewList = gridResampling(states, weights, rand, gType, sb);
		return statesNewList.toArray(new State[states.length]);
	}

	static List<State> gridResampling(State[] states, double[] weights, Random rnd, GridType gtype, StringBuilder sb){

		assert states.length == weights.length;

		int n = states.length;
		State[] statesNew = new State[n];

		double d = rnd.nextDouble(); // systematic resampling
		double[] u = new double[n];
		for(int k=0; k<n ; k++){
			if(gtype==GridType.stratified){
				d = rnd.nextDouble(); // stratified resampling
			}
			u[k] = ((double) k + d)/((double) n);
		}

		sb.setLength(0);
		sb.append("particle filter: no. of resampled particles ");
		double cumWeight = 0.0;
		int k = 0;
		for(int i=0; i<n; i++){
			int numSample = 0;
			cumWeight += weights[i];
			if(i==n-1){
				cumWeight = 1.0;
			}
			for( ; k<n; k++){
				if(u[k] < cumWeight){
					statesNew[k] = states[i];
					numSample++;
				}else{
					break;
				}
			}
			sb.append(numSample+",");
		}
		return Arrays.asList(statesNew);
	}

	public String toString(){
		return sb.toString();
	}

}
