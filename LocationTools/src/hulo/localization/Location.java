/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

/**
 * Data type to store two-dimensional location. (z indicates the floor number.)
 */
public class Location implements State, Serializable, Cloneable{

    float x;
    float y;
    float z; //corresponds to floor_num
    float h;
    String floor;

    double weight; // weight in particle filter
    double rssiBias = 0.0;

    public Location(){
    }


    public Location(double x, double y, double z){
        this.x=(float) x;
        this.y=(float) y;
        this.z=(float) z;
    }


    @Override
	public void setX(float x){ this.x = x; }

    @Override
	public void setY(float y){ this.y = y; }

    @Override
	public void setH(float h) { this.h = h;}

    @Override
	public void setZ(float z){ this.z = z; }

    public void setFloor(String floor) {this.floor = floor;}

    @Override
	public float getX(){ return x; }


    @Override
	public float getY(){ return y; }


    @Override
	public float getH(){ return h; }


    @Override
	public float getZ(){ return z; }


    public String getFloor(){ return floor;}

    public void setWeight(double weight){ this.weight = weight; }
    public double getWeight(){return weight;}

    public void setRssiBias(double rssiBias){ this.rssiBias = rssiBias;}
    public double getRssiBias() {return this.rssiBias;}

    @Override
	public boolean equals(State loc){
    	return (this.x==loc.getX())&&(this.y==loc.getY())&&(this.z==loc.getZ());
    }

    @Override
	public double getDistance(State loc){
        double distance;
        double x_loc = loc.getX();
        double y_loc = loc.getY();
        distance = Math.sqrt((x_loc-x)*(x_loc-x) +(y_loc-y)*(y_loc-y));
        return distance;
    }

	public double getFloorDifference(State loc){
        double diff;
        double z_loc = loc.getZ();
        diff = Math.abs(z_loc-z);
        return diff;
    }

    @Override
	public String toString(){
		return x+","+y+","+z;
	}

	@Override
	public void print(){
	    System.out.println("Location:(x,y,z)=("+x+","+y+","+z+")" );
	}

	public static double distance(State loc1, State loc2){
        double distance = loc1.getDistance(loc2);
        return distance;
    }

	public static double floorDifference(State loc1, State loc2){
		double floorDiff = loc1.getFloorDifference(loc2);
		return floorDiff;
	}

	/*
	public static Location mean(List<State> locations){
		return mean(locations.toArray(new State[0]));
	}
	*/

	public static Location var(List<State> locations){
		return var(locations.toArray(new State[0]));
	}


	public static Location mean(State[] locations){
		return mean(locations, false);
	}

	public static Location meanWeighted(State[] locations){
		return mean(locations, true);
	}

    public static Location mean(State[] locations, boolean isWeighted){
        float x_mean=0;
        float y_mean=0;
        float z_mean=0;
        float h_mean=0;
        int n_locations = locations.length;

        if(isWeighted){
            for(State loc: locations){
            	double w = isWeighted? loc.getWeight() : 1.0/((double) n_locations);
                x_mean += w*loc.getX();
                y_mean += w*loc.getY();
                z_mean += w*loc.getZ();
                h_mean += w*loc.getH();
            }
        }else{
            for(State loc: locations){
                x_mean += loc.getX();
                y_mean += loc.getY();
                z_mean += loc.getZ();
                h_mean += loc.getH();
            }
            x_mean/=n_locations;
            y_mean/=n_locations;
            z_mean/=n_locations;
            h_mean/=n_locations;
        }
        Location locNew = new Location(x_mean, y_mean, z_mean);
        locNew.setH(h_mean);
        return locNew;
    }

    public static Location var(State[] locations){
        State location_mean = Location.mean(locations);
        double x_mean = location_mean.getX();
        double y_mean = location_mean.getY();
        double z_mean = location_mean.getZ();
        double x_var=0;
        double y_var=0;
        double z_var=0;
        int n_locations = locations.length;
        for(State loc: locations){
        	double dx = (loc.getX()-x_mean);
        	double dy = (loc.getY()-y_mean);
        	double dz = (loc.getZ()-z_mean);
            x_var += dx*dx;
            y_var += dy*dy;
            z_var += dz*dz;
        }
        x_var/=n_locations;
        y_var/=n_locations;
        z_var/=n_locations;
        return new Location(x_var, y_var, z_var);
    }

    public static Location std(State[] locations){
    	Location varLoc = var(locations);
    	double xstd = Math.sqrt(varLoc.getX());
    	double ystd = Math.sqrt(varLoc.getY());
    	double zstd = Math.sqrt(varLoc.getZ());
    	Location stdLoc = new Location(xstd, ystd, zstd);
    	return stdLoc;
    }


    public static Location stdAxis(State[] locations, double angle){
    	Location meanLoc = mean(locations);
    	double xm = meanLoc.getX();
    	double ym = meanLoc.getY();
    	//double zm = meanLoc.getZ();
    	int n = locations.length;
    	// Axis transformation
    	State[] locs = new State[n];
    	for(int i=0; i<n; i++){
    		double x = locations[i].getX();
    		double y = locations[i].getY();
    		double z = locations[i].getZ();
    		double xnew = Math.cos(angle)*(x-xm) + Math.sin(angle)*(y-ym);
    		double ynew = - Math.sin(angle)*(x-xm) + Math.cos(angle)*(y-ym);
     		locs[i] = new Location(xnew,ynew,z);
    	}
    	return std(locs);
    }


    @Override
    public Location clone(){
    	try{
    		return (Location) super.clone();
    	}catch(CloneNotSupportedException e){
    		throw new InternalError(e.toString());
    	}
    }

    public JSONObject toJSONObject(){
    	JSONObject json=null;
    	try {
    		json = new JSONObject();
			json.put("x", this.getX()).put("y", this.getY()).put("z", this.getZ());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
    }

    public static JSONArray statesToJSONArray(State[] states){
    	JSONArray jarray = null;
    	jarray = new JSONArray();
		for(State s: states){
			if(s instanceof Location){
				JSONObject jobj = ((Location) s ).toJSONObject();
				JSONArray array = new JSONArray();
				try {
					array.add(Math.round(jobj.getDouble("x")*100)/100.0);
					array.add(Math.round(jobj.getDouble("y")*100)/100.0);
					array.add(Math.round(jobj.getDouble("z")*100)/100.0);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				jarray.add(array);
			}
		}
		return jarray;
    }

    public static double getOrientationDiff(double o1 , double o2){
        double ret = o2-o1;
        if (ret > Math.PI) {
            ret -= Math.PI*2;
        }
        if (ret < -Math.PI) {
            ret += Math.PI*2;
        }
        return ret;
    }

    public static State findClosest(State loc, State[] locs) {
    	State[] locsK = findClosest(loc, locs, 1);
    	return locsK[0];
    }

    public static State[] findClosest(State loc, State[] locs, int k) {
    	final State locKey = loc;
    	Arrays.sort(locs, new Comparator<State>(){
			@Override
			public int compare(State o1, State o2) {
				double d1 = o1.getDistance(locKey);
				double fd1 = o1.getFloorDifference(locKey);
				double d2 = o2.getDistance(locKey);
				double fd2 = o2.getFloorDifference(locKey);
				if(fd1 < fd2){
					return -1;
				}else if(fd1 > fd2){
					return 1;
				}else{
					return Double.compare(d1, d2);
				}
			}
    	});
    	return Arrays.copyOfRange(locs, 0, k);
    }

    public static State[] shuffleStates(State[] states){
    	List<State> stateList = Arrays.asList(states);
    	Collections.shuffle(stateList);
    	State[] statesNew = stateList.toArray(new State[states.length]);
    	return statesNew;
    }
}

