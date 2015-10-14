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

package hulo.localization.models.sys;

import hulo.localization.State;

import java.io.InputStream;

public class RandomWalkerOnFloorsFree extends RandomWalker {

	static double pUp = 0.1;
	static double pDown = 0.1;

	public RandomWalkerOnFloorsFree() {
		super();
	}

	public RandomWalkerOnFloorsFree(double sigma){
		super(sigma);
	}

	public RandomWalkerOnFloorsFree(InputStream is){
		super(is);
	}

	@Override
	public State predict(State state){
        return predict(state, sigma, isWalking);
	}


	public static State predict(State state, double sigma, boolean isWalking){
		State stateTmp = predict2D(state, sigma);
		return predictZ(stateTmp, isWalking);
	}

	public static State predict2D(State state, double sigma){
		double x = state.getX();
        double y = state.getY();
        // Assuming 2D random walk
        x += sigma*getRandom().nextGaussian();
        y += sigma*getRandom().nextGaussian();
        State stateNew = (State) state.clone();
        stateNew.setX((float) x);
        stateNew.setY((float) y);
        return stateNew;
	}

	public static double predictZ(double z){
		return predictZ(z, pUp, pDown);
	}

	protected static double predictZ(double z, double pUp, double pDown){
		double znew = z;
        double prob = getRandom().nextDouble();
        if(prob<pUp){
        	znew += 1;
        }else if(prob < pUp + pDown){
    		znew -= 1;
    	}
        return znew;
	}

	public static State predictZ(State state, boolean isWalking){
		if(isWalking){
			double z = state.getZ();
			z = predictZ(z);
			State stateNew = (State) state.clone();
			stateNew.setZ((float) z);
			return stateNew;
		}else{
			return state;
		}
	}

}
