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

package hulo.localization;

import hulo.localization.Pose.WalkState;
import hulo.localization.models.sys.RandomWalkerOnFloorsFree;
import hulo.localization.utils.RandomExtended;

public class PoseMotion implements StateTransition{

	static final double dt = 1.0;
	static double smallNSteps = 0.01;

	RandomExtended randEx = new RandomExtended();
	PoseSetting setting = null;

	public PoseSetting getSetting() {
		return setting;
	}

	public void setSetting(PoseSetting setting) {
		this.setting = setting;
	}

	protected PoseMotion(){}

	public static PoseMotion create(PoseSetting setting){
		PoseMotion poseMotion = new PoseMotion();
		poseMotion.setSetting(setting);
		return poseMotion;
	}

	double updateNSteps(double nSteps){
		return randEx.nextTruncatedNormal(nSteps,
									getSetting().stdNSteps,
									getSetting().minNSteps,
									getSetting().maxNSteps);
	}

	double updateStepLength(double stepLength){
		return randEx.nextTruncatedNormal(stepLength,
									getSetting().stdStepLength,
									getSetting().minStepLength,
									getSetting().maxStepLength);
	}

	double updateVelocityRep(double velocityRep){
		return randEx.nextTruncatedNormal(velocityRep,
									getSetting().driftVelocityRep, // different from stdVelocityRep
									getSetting().minVelocityRep,
									getSetting().maxVelocityRep);
	}

	double updateRssiBias(double rssiBias){
		return rssiBias + getSetting().stdRssiBias*randEx.nextGaussian();
	}

	WalkState updateWalkState(double nSteps){
		return nSteps<smallNSteps? WalkState.stay : WalkState.walk;
	}



	@Override
	public State predict(State state) {
		return predict((Pose) state);
	}

	public Pose predict(Pose pose){
		Pose poseNew = pose.clone();

		// update rssiBias
		poseNew.rssiBias = updateRssiBias(pose.getRssiBias());

		// update velocity
		if(getSetting().velocityMode.equals(PoseSetting.STEPLENGTH_TIMES_NSTEPS)){
			poseNew.setNSteps(updateNSteps(pose.getNSteps()));
			poseNew.stepLength = updateStepLength(pose.stepLength);
			poseNew.velocity = poseNew.stepLength * poseNew.nSteps;
		}else if(getSetting().velocityMode.equals(PoseSetting.CONSTANT_VELOCITY)){
			Pose.WalkState walkState = updateWalkState(poseNew.nSteps);
			if(walkState == Pose.WalkState.stay){
				poseNew.velocity = 0.0;
			}else if(walkState == Pose.WalkState.walk){
				poseNew.velocityRep = updateVelocityRep(poseNew.velocityRep);
				poseNew.velocity = poseNew.velocityRep*poseNew.fieldVelocityRate;
			}else{
				System.out.println("walkState is not specified");
			}
		}else{
			System.out.println("velocityMode is not specified");
		}

		// update orientation
		poseNew = updateOrientatin(poseNew);

		// update position
		poseNew = updatePosition(poseNew);

		return poseNew;
	}

	protected Pose updateOrientatin(Pose pose){
		Pose poseNew = pose.clone();
		if(randEx.nextDouble()<getSetting().resetRate){
			poseNew = randamInitializePose(poseNew);
			poseNew.orientationMeasured += getSetting().stdOrientation*randEx.nextGaussian() ;
		}else{
			poseNew.orientationBias += getSetting().stdOrientationBias*randEx.nextGaussian();
			poseNew.orientationMeasured += getSetting().stdOrientation*randEx.nextGaussian();
		}
		poseNew.orientation = poseNew.orientationBias + poseNew.orientationMeasured;
		return poseNew;
	}

	protected Pose updatePosition(Pose pose){
		Pose poseNew = pose.clone();
		double vx = poseNew.velocity * Math.cos(poseNew.orientation);
		double vy = poseNew.velocity * Math.sin(poseNew.orientation);
		poseNew.x += vx*dt;
		poseNew.y += vy*dt;
		return poseNew;
	}

	public Pose predictZ(Pose pose){
		Pose poseNew = pose.clone();
		poseNew.z = (float) RandomWalkerOnFloorsFree.predictZ(pose.z);
		return poseNew;
	}

	public Pose moveWithAngle(Pose pose, double angle) {
		Pose poseNew =  predict(pose);
		double vx = poseNew.getVelocity() * Math.cos(angle);
		double vy = poseNew.getVelocity() * Math.sin(angle);
		poseNew.x = (float) (pose.x + vx*dt);
		poseNew.y = (float) (pose.y + vy*dt);
		return poseNew;
	}

	public <T extends Location>Pose createFrom(T loc){
		return createFrom(loc, this.getSetting());
	}

	private <T extends Location>Pose createFrom(T loc, PoseSetting poseSetting){

		double x = loc.getX();
		double y = loc.getY();
		double z = loc.getZ();
		Pose pose = new Pose(x,y,z);
		pose.setH(loc.getH());
		pose.setFloor(loc.getFloor());
		pose.setWeight(loc.getWeight());

		pose = randamInitializePose(pose);
		return pose;
	}


	public Pose randamInitializePose(Pose pose){
		pose.orientationBias = 2.0*Math.PI*randEx.nextDouble();
		if(getSetting().constantsPrior.equals(PoseSetting.UNIFORM_DISTRIBUTION)){
			pose.stepLength = getSetting().minStepLength + (getSetting().maxStepLength-getSetting().minStepLength)*randEx.nextDouble();
			// representative velocity used in CONSTANT_VELOCITY mode
			pose.velocityRep = getSetting().minVelocityRep + (getSetting().maxVelocityRep-getSetting().minVelocityRep)*randEx.nextDouble();
		}else if(getSetting().constantsPrior.equals(PoseSetting.TRUNCATED_NORMAL_DISTRIBUTION)){
			pose.stepLength = randEx.nextTruncatedNormal(getSetting().meanStepLength,
					getSetting().stdStepLength,
					getSetting().minStepLength,
					getSetting().maxStepLength);
			// representative velicity used in CONSTANT_VELOCITY mode
			pose.velocityRep = randEx.nextTruncatedNormal(getSetting().meanVelocityRep,
					getSetting().stdVelocityRep,
					getSetting().minVelocityRep,
					getSetting().maxVelocityRep);
		}
		pose.rssiBias = getSetting().meanRssiBias + getSetting().stdRssiBias*randEx.nextGaussian();
		return pose;
	}

}
