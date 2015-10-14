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

package hulo.localization.models;

import hulo.localization.BLEBeacon;
import hulo.localization.Beacon;
import hulo.localization.Location;
import hulo.localization.Sample;
import hulo.localization.State;

import java.util.ArrayList;
import java.util.List;

public class ModelAdaptUtils {

	static final int NDIM=3;

	private ModelAdaptUtils(){
		throw new AssertionError();
	}


	public static double[][] samplesToLocationsMat(List<Sample> samples){
		List<Location> locations = Sample.samplesToLocations(samples);
		return locationsToMat(locations.toArray(new State[0]));
	}

	public static double[][] samplesToBeaconsListMat(List<Sample> samples, double minRssi, int n_max){
		List<List<Beacon>> beaconsList = Sample.samplesToBeaconsList(samples);
		return beaconsListToMat(beaconsList, minRssi, n_max);
	}

	public static double[] locationToVec(State state){
		double[] x = new double[NDIM];
		x[0] = state.getX();
		x[1] = state.getY();
		x[2] = state.getZ();
		return x;
	}

	public static double[] biasesToVec(State[] states){
		int n = states.length;
		double[] biases = new double[n];
		for(int i=0; i<n; i++){
			biases[i] = states[i].getRssiBias();
		}
		return biases;
	}

	public static Location vecToLocation(double[] x){
		assert(x.length==NDIM);
		Location loc = new Location(x[0], x[1], x[2]);
		return loc;
	}


	public static double[][] locationsToMat(List<State> states){
		return locationsToMat(states.toArray(new State[0]));
	}

	public static double[][] locationsToMat(State[] states){
		int n_sample = states.length;
		double[][] X = new double[n_sample][NDIM];
		for(int i=0; i<n_sample;i++){
			X[i] = locationToVec(states[i]);
		}
		return X;
	}

	public static double[] beaconsToVec(List<Beacon> beacons, double minRssi, int n_max){
		double[] vec = new double[n_max];
		for(int i=0; i<n_max; i++){
			vec[i]=minRssi;
		}
		for(Beacon beacon: beacons){
			int id = beacon.getId();
			double rssi = beacon.getRssi();
			vec[id]=rssi;
		}
		return vec;
	}

	public static List<Beacon> vecToBeacons(double[] y, double minRssi, int n_max){
		List<Beacon> beacons = new ArrayList<Beacon>();
		for(int i=0; i<n_max; i++){
			int id = i;
			double rssi = y[i];
			if(minRssi<rssi){
				int major = 0;
				int minor = id;
				Beacon beacon = new Beacon(major, minor, (float)rssi);
				beacons.add(beacon);
			}
		}
		return beacons;
	}


	public static double[][] beaconsListToMat(List<List<Beacon>> beaconsList, double minRssi, int n_max){
		int n_sample = beaconsList.size();
		double[][] mat = new double[n_sample][n_max];

		for(int i=0; i<n_sample; i++){
			List<Beacon> beacons = beaconsList.get(i);
			mat[i] = beaconsToVec(beacons, minRssi, n_max);
		}

		return mat;
	}

	public static double[] beaconsToVecSubset(List<Beacon> beacons){
		int nBeacons = beacons.size();
		double[] vec = new double[nBeacons];
		for(int i=0; i<nBeacons; i++){
			vec[i]=beacons.get(i).getRssi();
		}
		return vec;
	}

	public static double[][] beaconsListToMatSubset(List<List<Beacon>> beaconsList, List<Beacon> beacons, double minRssi, int n_max){
		int n_sample = beaconsList.size();
		int nActive = beacons.size();
		double[][] matOrigin = beaconsListToMat(beaconsList, minRssi, n_max);
		double[][] mat = new double[n_sample][nActive];

		for(int i=0; i<n_sample; i++){
			for(int j=0; j<nActive; j++){
				int id = beacons.get(j).getId();
				double rssi = matOrigin[i][id];
				mat[i][j]=rssi;
			}
		}
		return mat;
	}

	public static double[][] beaconsListToMatSubset(List<List<Beacon>> beaconsList, int[] beaconIds, double minRssi, int n_max){
		int n_sample = beaconsList.size();
		double[][] matOrigin = beaconsListToMat(beaconsList, minRssi, n_max);
		double[][] mat = new double[n_sample][beaconIds.length];
		for(int i=0; i<n_sample; i++){
			for(int j=0; j<beaconIds.length; j++){
				int id = beaconIds[j];
				double rssi = matOrigin[i][id];
				mat[i][j]=rssi;
			}
		}
		return mat;
	}



	public static double[][] BLEBeaconsToMat(List<BLEBeacon> bleBeacons, int n_max){
		/*
		 * Output X[number_beacons][number of dimensiton]
		 * x, y, z
		 * 0  0  4
		 *
		 */
		double[][] bleBeaconLocs = new double[n_max][3];
		for(BLEBeacon bleBeacon: bleBeacons){
			int id = bleBeacon.getId();
			bleBeaconLocs[id] = locationToVec(bleBeacon);
		}
		return bleBeaconLocs;
	}

	public static double[][] BLEBeaconsToMatSubset(List<BLEBeacon> bleBeacons, List<Beacon> beacons, int n_max){
		/*
		 * Output X[number_beacons][number of dimensiton]
		 * x, y, z
		 * 0  0  4
		 *
		 */
		double[][] bleBeaconLocsOrigin = BLEBeaconsToMat(bleBeacons, n_max);
		int nActive = beacons.size();
		double[][] bleBeaconLocs = new double[nActive][NDIM];
		for(int i=0; i<nActive; i++){
			for(int j=0; j<NDIM; j++){
				int id = beacons.get(i).getId();
				bleBeaconLocs[i][j] = bleBeaconLocsOrigin[id][j];
			}
		}
		return bleBeaconLocs;
	}

	public static double[][] BLEBeaconsToMatSubset(List<BLEBeacon> bleBeacons, int[] beaconIds, int n_max){
		double[][] bleBeaconLocsOrigin = BLEBeaconsToMat(bleBeacons, n_max);
		double[][] bleBeaconLocs = new double[beaconIds.length][NDIM];
		for(int i=0; i<beaconIds.length; i++){
			for(int j=0; j<NDIM; j++){
				int id = beaconIds[i];
				bleBeaconLocs[i][j] = bleBeaconLocsOrigin[id][j];
			}
		}
		return bleBeaconLocs;
	}


	public static int[] beaconsToActiveBeaconArray(List<Beacon> beacons){
		int n = beacons.size();
		int[] activeBeaconTable = new int[n];
		for(int i=0; i<n; i++){
			int id = beacons.get(i).getId();
			activeBeaconTable[i] = id;
		}
		return activeBeaconTable;
	}


	public static int[] BLEBeaconsToActiveBeaconArray(List<BLEBeacon> bleBeacons){
		int n = bleBeacons.size();
		int[] activeBeaconTable = new int[n];
		for(int i=0; i<n; i++){
			int id = bleBeacons.get(i).getId();
			activeBeaconTable[i] = id;
		}
		return activeBeaconTable;
	}

}
