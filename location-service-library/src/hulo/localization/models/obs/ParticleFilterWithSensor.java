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
import hulo.localization.LocalizationInput;
import hulo.localization.LocalizationStatus;
import hulo.localization.Location;
import hulo.localization.Pose;
import hulo.localization.State;
import hulo.localization.models.sys.PoseSystemModel;
import hulo.localization.models.sys.RandomWalker;
import hulo.localization.models.sys.SystemModel;

class ParticleFilterWithSensor extends ParticleFilter implements ObservationModel, ObservationModelWithSensor{

	public ParticleFilterWithSensor(SystemModel systemModel, ObservationModel observationModel){
    	super(systemModel,observationModel);
    }

	@Override
    LocalizationStatus resetStatus(LocalizationInput locInput){
    	LocalizationStatus locStatus =super.resetStatus(locInput);
    	if(locStatus==null){
    		return null;
    	}

		Double orientationInit = locInput.getOrientationInitial();
		if(orientationInit!=null){
			System.out.println("orientationInit="+orientationInit);
			State[] states = locStatus.getStates();
			State[] statesNew = new State[states.length];
			for(int i=0; i<states.length; i++){
				if(systemModel instanceof PoseSystemModel){
					State stateNew = ((PoseSystemModel) systemModel).toPoseData(states[i]);
					((Pose) stateNew).setOrientationInitial(orientationInit);
					statesNew[i] = stateNew;
				}else{
					statesNew[i] = states[i];
				}
			}
			locStatus.setStates(statesNew);
		}

		return locStatus;
    }

    @Override
    LocalizationStatus correctStatus(LocalizationStatus locStatus, LocalizationInput locInput){
    	locStatus = super.correctStatus(locStatus, locInput);
    	State[] states = locStatus.getStates();
    	if(systemModel instanceof PoseSystemModel){
    		locStatus.setMeanState(Pose.mean(states, usesWeighted));
    	}
    	return locStatus;
    }


    @Override
    LocalizationStatus initializeLocalizationStatus(LocalizationInput locInput){
    	// Random initialiation
    	LocalizationStatus locStatus = super.initializeLocalizationStatus();
    	// Update
    	locStatus = super.update(locStatus, locInput);
    	State[] states = locStatus.getStates();
    	// Transform to PoseData
    	State[] statesNew = new State[states.length];
    	for(int i=0; i<statesNew.length ; i++){
    		if(systemModel instanceof PoseSystemModel){
    			statesNew[i] = ((PoseSystemModel) systemModel).toPoseData(states[i]);
    		}else if(systemModel instanceof RandomWalker){
    			statesNew[i] = states[i];
    		}
    	}
        locStatus.setLocation(Location.mean(statesNew, usesWeighted));
        if(systemModel instanceof PoseSystemModel){
        	locStatus.setMeanState(Pose.mean(statesNew, usesWeighted));
        }
    	locStatus.setStates(statesNew);
    	return locStatus;
    }

    @Override
    public double[] getLogLikelihoods(LocalizationInput locInput, State[] locations){
    	return super.getLogLikelihoods(locInput, locations);
    }

    @Override
    public String toString(){
    	return "ParticleFilterWithSensor["+systemModel+","+observationModel+"], N="+numParticles+", alphaWeaken="+alphaWeaken;
    }

}
