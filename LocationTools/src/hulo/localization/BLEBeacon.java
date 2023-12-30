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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BLEBeacon extends Location implements Serializable, Cloneable{

	static int idForUnknown = -1;

	int id;
	int origId;
	String uuid;
	int minor = -1;
	int major = -1;

	int outputPower = 0;
	int msdPower = 0;

    public void setId(int id){
    	this.id=id;
    }

    public int getId(){ return id; }

    public void setUuid(String uuid){
    	this.uuid = uuid;
    }
    public String getUuid(){
    	return this.uuid;
    }

    public void setMinor(int minor){ this.minor = minor; }
    public int getMinor(){return minor;}

    public void setMajor(int major){ this.major = major; }
    public int getMajor(){return major;}

    public void setOutputPower(int outputPower){ this.outputPower = outputPower; }
    public int getOutputPower(){ return outputPower;}

    public void setMsdPower(int msdPower){ this.msdPower = msdPower; }
    public int getMsdPower(){ return msdPower ;}

    public BLEBeacon(int id, double x, double y, double z){
        this.id=id;
        this.origId = id;
        this.x=(float)x;
        this.y=(float)y;
        this.z=(float)z;
    }

    public BLEBeacon(int id, int major, int minor, double x, double y, double z){
    	 this.id=id;
    	 this.origId = id;
    	 this.major = major;
    	 this.minor = minor;
         this.x=(float)x;
         this.y=(float)y;
         this.z=(float)z;
    }

    public String toString(){
    	return id+","+major+","+minor+","+x+","+y+","+z;
    }

    public void print(){
        System.out.println("BLEBeacon:(id,major,minor,x,y,z)=("+this+")");
    }


    protected static Random rand = new Random();
    public static void setRandom(Random rnd){
    	rand = rnd;
    }


    public static void reassignShortestIds(List<BLEBeacon> bleBeacons){
    	Collections.sort(bleBeacons, new Comparator<BLEBeacon>(){
			@Override
			public int compare(BLEBeacon b1, BLEBeacon b2) {
				int major1 = b1.getMajor();
				int minor1 = b1.getMinor();

				int major2 = b2.getMajor();
				int minor2 = b2.getMinor();

				if(major1!=major2){
					return Integer.compare(major1, major2);
				}else{
					return Integer.compare(minor1, minor2);
				}
			}
    	});
    	int nb=bleBeacons.size();
    	for(int i=0; i<nb ; i++){
    		BLEBeacon BLEBeacon = bleBeacons.get(i);
    		BLEBeacon.setId(i);
    	}
    }

    public static void setBLEBeaconIdsToMeasuredBeacons(List<BLEBeacon> bleBeacons, List<Beacon> beacons){
    	StringBuilder sbUnknown = new StringBuilder();
    	int n = beacons.size();
    	for(int i=0; i<n; i++){
    		Beacon b = beacons.get(i);
    		int major = b.getMajor();
    		int minor = b.getMinor();
    		double rssi = b.getRssi();
    		boolean isUpdated = false;
    		for(BLEBeacon ib: bleBeacons){
    			int ma = ib.getMajor();
    			int mi = ib.getMinor();
    			int id = ib.getId();
    			if(ma==major && mi==minor){
    				b.setId(id);
    				isUpdated = true;
    				break;
    			}
    		}
    		if(! isUpdated){
    			if(! (rssi==Beacon.minRssi)){
    				sbUnknown.append("("+major+","+minor+","+rssi+")");
    			}
    			beacons.remove(i);
    			i--;
    		}
    		n = beacons.size();
    	}
    	if( ! sbUnknown.toString().equals("")){
    		System.out.println("Observed unregistered beacons: (major,minor,rssi)=" + sbUnknown.toString());
    	}
    }


    public static void setBLEBeaconIdsToSample(List<BLEBeacon> bleBeacons, Sample sample){
    	setBLEBeaconIdsToMeasuredBeacons(bleBeacons, sample.getBeacons());
    }

    public static void setBLEBeaconIdsToSamples(List<BLEBeacon> bleBeacons, List<Sample> samples){
    	for(Sample s: samples){
    		setBLEBeaconIdsToSample(bleBeacons, s);
    	}
    }

    @Override
    public BLEBeacon clone(){
    	return (BLEBeacon) super.clone();
    }

    public static BLEBeacon find(List<BLEBeacon> bleBeacons, int minor){
    	Map<Integer, BLEBeacon> map = new HashMap<Integer, BLEBeacon>();
    	for(BLEBeacon ib: bleBeacons){
    		int _minor = ib.getMinor();
    		map.put(_minor, ib);
    	}
    	return map.get(minor);
    }

}
