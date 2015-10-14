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

package hulo.localization.synthe;

import hulo.localization.BLEBeacon;
import hulo.localization.Beacon;
import hulo.localization.Location;
import hulo.localization.Sample;
import hulo.localization.kernels.GaussianKernel;
import hulo.localization.models.ModelAdaptUtils;
import hulo.localization.utils.JSONUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class SyntheticBeaconDataGenerator {
	Random rand = new Random();

	long timeStampInit = 1444834800;
	long dTS = 1000;

	double sigma = 3.0;

	public void setRandom(Random rand){
		this.rand = rand;
	}

	public SyntheticBeaconDataGenerator(){}

	public SyntheticBeaconDataGenerator(JSONObject json){
		setJSON(json);
	}

	public SyntheticBeaconDataGenerator(double sigma){
		setSigma(sigma);
	}

	public void setSigma(double sigma){
		this.sigma = sigma;
	}

	JSONObject gpJSON;
	JSONObject ldplJSON;
	JSONObject noiseJSON;

	public SyntheticBeaconDataGenerator setJSON(JSONObject json){
		gpJSON = json.optJSONObject("GP");
		ldplJSON = json.optJSONObject("LDPL");
		noiseJSON = json.optJSONObject("Noise");
		return this;
	}

	static class LDPL{

		static final String N_STR = "n";
		static final String A_STR = "A";
		static final String FA = "fa";
		static final String FB = "fb";
		static final String DIST_OFFSET = "distOffset";

		double n = 2.0; //Free space
		double A = -70;
		double fa = 4;
		double fb = 15;

		double distOffset = 0.1;
		BLEBeacon BLEBeacon;

		public void setBLEBeacon(BLEBeacon BLEBeacon){
			this.BLEBeacon = BLEBeacon;
		}


		public LDPL settingFromJSON(JSONObject json){
			n = json.optDouble(N_STR);
			A = json.optDouble(A_STR);
			fa = json.optDouble(FA);
			fb = json.optDouble(FB);
			distOffset = json.optDouble(DIST_OFFSET);
			return this;
		}

		public double predict(Location loc){
			double distance = loc.getDistance(BLEBeacon);
			double floorDiff = loc.getFloorDifference(BLEBeacon);

			distance += distOffset;
			double y = - 10*n * Math.log10(distance) + A;
			if(floorDiff > 0){
				y += fa*(floorDiff-1)+fb;
			}
			return y;
		}
	}



	static class NoiseGenerator{

		static final String SIGMA = "sigma";

		double sigma = 3.0;

		Random rand;

		public NoiseGenerator settingFromJSON(JSONObject json){
			Double sigma = json.optDouble(SIGMA);
			if(sigma!=null){
				this.sigma = sigma;
			}
			return this;
		}

		public NoiseGenerator random(Random rand){
			this.rand = rand;
			return this;
		}

		public NoiseGenerator setSigma(double sigma){
			this.sigma = sigma;
			return this;
		}

		public double predict(){
			return sigma*rand.nextGaussian();
		}
	}


	static class GP{

		double sigma_a = 2.0;
		double[] lengthes = {1.0,1.0, 0.01};

		double sigma_n = 0.0001;

		Random rand;

		GaussianKernel kernel = new GaussianKernel();

		GP(){
			kernel.setStdev(sigma_a);
			kernel.setLengthes(lengthes);
		}

		List<Location> locations;
		double[] y;
		double[] alphas;

		public GP random(Random rand){
			this.rand = rand;
			return this;
		}

		static final String SIGMA_A = "sigma_a";
		static final String LENGTHES = "lengthes";
		static final String SIGMA_N = "sigma_n";

		public GP settingFromJSON(JSONObject json){
			try {
				sigma_a = json.getDouble(SIGMA_A);
				lengthes = JSONUtils.array1DfromJSONArray(json.getJSONArray(LENGTHES));
				sigma_n = json.getDouble(SIGMA_N);
				kernel.setStdev(sigma_a);
				kernel.setLengthes(lengthes);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return this;
		}

		public JSONObject toJSONObject(){

			JSONObject json = new JSONObject();
			try{
				json.put(SIGMA_A, sigma_a);
				json.put(LENGTHES, lengthes);
				json.put(SIGMA_N, sigma_n);
			}catch(JSONException e){
				e.printStackTrace();
			}

			return null;
		}

		public void setLocations(List<Location> locations){
			this.locations = locations;
		}

		public void fit(List<Location> locations){
			setLocations(locations);
			int n = locations.size();
			double[][] K = new double[n][n];

			for(int i=0; i<n; i++){
				Location loc1 = locations.get(i);
				double[] x1 = ModelAdaptUtils.locationToVec(loc1);
				for(int j=i; j<n; j++){
					Location loc2 = locations.get(j);
					double[] x2 = ModelAdaptUtils.locationToVec(loc2);
					double k =kernel.computeKernel(x1, x2);
					K[i][j] = k;
					K[j][i] = k;
				}
			}
			RealMatrix Kmat = MatrixUtils.createRealMatrix(K);
			RealMatrix lambdamat = MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(sigma_n*sigma_n); //Tentative treatment

			RealMatrix Kymat = Kmat.add(lambdamat);

			CholeskyDecomposition chol = new CholeskyDecomposition(Kymat);
			RealMatrix Lmat = chol.getL();

			double[] normalRands = new double[n];
			for(int i=0; i<n; i++){
				normalRands[i] = rand.nextGaussian();
			}
			this.y = Lmat.operate(normalRands);

			RealMatrix invKymat = (new LUDecomposition(Kymat)).getSolver().getInverse();
			this.alphas = invKymat.operate(y);
		}


		public double predict(Location loc){
			int n = locations.size();
			double[] kstar = new double[n];

			double[] xstar = ModelAdaptUtils.locationToVec(loc);
			for(int i=0; i<n; i++){
				double[] xi = ModelAdaptUtils.locationToVec(locations.get(i));
				kstar[i] = kernel.computeKernel(xstar, xi);
			}
			double pred = 0;
			for(int i=0; i<n; i++){
				pred += alphas[i]*kstar[i];
			}
			return pred;
		}

	}


	static class WallAttenuator{
		BLEBeacon bleBeacon;
		List<Wall> walls = new ArrayList<>();

		public WallAttenuator setBLEBeacon(BLEBeacon bleBeacon){
			this.bleBeacon = bleBeacon;
			return this;
		}

		public WallAttenuator setWalls(List<Wall> walls){
			if(walls!=null){
				this.walls = walls;
			}
			return this;
		}

		public double predict(Location loc){
			double attenuation  = 0;
			for(Wall wall: walls){
				attenuation += wall.computeAttenuation(this.bleBeacon, loc);
			}

			return attenuation;
		}
	}



	public List<BLEBeacon> generateBLEBeacons(int n){
		int major = 1;
		List<Location> locs = generateLocations(n);
		List<BLEBeacon> ibs = new ArrayList<>();
		for(int i=0; i<n; i++){
			Location loc = locs.get(i);
			int id = i+1;
			int minor = id;
			//BLEBeacon ib = new BLEBeacon(id, loc.getX(), loc.getY(), loc.getZ());
			BLEBeacon ib = new BLEBeacon(id, major, minor, loc.getX(), loc.getY(), loc.getZ());
			ibs.add(ib);
		}
		return ibs;
	}


	double xmin = 0;
	double xmax = 20;
	double ymin = 0;
	double ymax = 20;

	public SyntheticBeaconDataGenerator setXmin(double xmin){
		this.xmin = xmin;
		return this;
	}
	public SyntheticBeaconDataGenerator setXmax(double xmax){
		this.xmax = xmax;
		return this;
	}
	public SyntheticBeaconDataGenerator setYmin(double ymin){
		this.ymin = ymin;
		return this;
	}
	public SyntheticBeaconDataGenerator setYmax(double ymax){
		this.ymax = ymax;
		return this;
	}


	public List<Location> generateLocations(int n){

		int zmin = 0;
		int zmax = 0;

		List<Location> locs = new ArrayList<>();

		for(int i=0; i<n; i++){
			double x = xmin + (xmax-xmin)*rand.nextDouble();
			double y = ymin + (ymax-ymin)*rand.nextDouble();
			double z = zmin + rand.nextInt(zmax-zmin+1);
			Location loc = new Location(x, y, z);
			locs.add(loc);
		}
		return locs;

	}

	public List<Wall> generateWalls(int n){
		List<Location> locs1 = generateLocations(n);
		List<Location> locs2 = generateLocations(n);

		List<Wall> walls = new ArrayList<>();
		for(int i=0; i<n; i++){
			Location l1 = locs1.get(i);
			Location l2 = locs2.get(i);
			Wall wall = new Wall.Builder(l1.getX(), l1.getY(), l2.getX(), l2.getY()).build() ;
			walls.add(wall);
		}
		return walls;
	}


	List<Sample> samples = new ArrayList<>();

	List<Location> locations;
	List<BLEBeacon> bleBeacons;

	public void setBLEBeacons(List<BLEBeacon> bleBeacons){
		this.bleBeacons = bleBeacons;
	}

	public void setLocations(List<Location> locations){
		this.locations = locations;
	}

	Map<Integer, LDPL> ldpls = new HashMap<>();
	Map<Integer, GP> gps = new HashMap<>();
	Map<Integer, WallAttenuator> wallAtts = new HashMap<>();
	NoiseGenerator noise;
	String uuid;
	public SyntheticBeaconDataGenerator fit(List<Location> locations, List<BLEBeacon> bleBeacons, List<Wall> walls){
		uuid = bleBeacons.get(0).getUuid();
		this.bleBeacons = bleBeacons;

		noise = new NoiseGenerator().settingFromJSON(noiseJSON).random(rand);

		List<Location> locsFit = getUniqueLocations(locations);
		for(BLEBeacon ib: bleBeacons){
			int id = ib.getId();
			LDPL ldpl = new LDPL().settingFromJSON(ldplJSON);
			ldpl.setBLEBeacon(ib);
			ldpls.put(id, ldpl);

			GP gp = new GP().settingFromJSON(gpJSON).random(rand);
			gp.fit(locsFit);
			gps.put(id, gp);

			WallAttenuator wallAtt = new WallAttenuator().setBLEBeacon(ib).setWalls(walls);
			wallAtts.put(id, wallAtt);
		}
		return this;
	}

	List<Location> getUniqueLocations(List<Location> locations){
		List<Location> locsNew = new ArrayList<>();
		Location locPre = locations.get(0);
		locsNew.add(locPre);
		for(Location loc: locations){
			if(! locPre.equals(loc)){
				locsNew.add(loc);
			}
			locPre = loc;
		}
		return locsNew;
	}


	public List<Sample> generateSamples(List<Location> locations){
		long timestamp = timeStampInit;
		List<Sample> samples = new ArrayList<>();
		for(Location loc: locations){
			List<Beacon> beacons = new ArrayList<>();
			for(BLEBeacon ib : bleBeacons){
				int id = ib.getId();
				double rssi = ldpls.get(id).predict(loc);
				rssi += gps.get(id).predict(loc);
				rssi += wallAtts.get(id).predict(loc);
				rssi += noise.predict();

				long rssiLong = Math.round(rssi);
				if(Beacon.minRssi<rssi && rssi<Beacon.maxRssi){
					Beacon b = new Beacon(ib.getMajor(), ib.getMinor(), (float) rssiLong);
					beacons.add(b);
				}
			}
			Sample smp = new Sample(loc, uuid, beacons);
			smp.setTimeStamp(timestamp);
			samples.add(smp);
			timestamp += dTS;
		}
		this.samples = samples;
		return samples;
	}


	public List<Sample> generateSamples(int nLoc, int nBeacon, int nWalls){
		List<BLEBeacon> bleBeacons = generateBLEBeacons(nBeacon);
		List<Location> locations = generateLocations(nLoc);
		List<Wall> walls = generateWalls(nWalls);
		return this.fit(locations, bleBeacons, walls).generateSamples(locations);
	}


	public void print(){
		StringBuilder sb = new StringBuilder();
		for(Sample smp: samples){
			sb.append("uuid="+smp.getUuid()+",");
			Location loc = smp.getLocation();
			List<Beacon> bs = smp.getBeacons();
			sb.append("location="+loc.toString());
			sb.append(", ");
			sb.append("Beacons=");
			for(Beacon b: bs){
				sb.append("("+b.toString()+"),");
			}
			sb.append("\n");
		}
		System.out.println(sb.toString());
	}
}
