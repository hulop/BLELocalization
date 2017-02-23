<!--
The MIT License (MIT)

Copyright (c) 2014, 2015 IBM Corporation
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
-->


# BLE Localization

This is a Bluetooth LE localization library to locate smartphones by estimating position from beacon signal strength.
The localization algorithm is based on Monte Carlo localization using a particle filter which integrates radio wave signal strength and various sensor data of smartphones to localize them.

## About
[About HULOP](https://github.com/hulop/00Readme)

## License
[MIT](http://opensource.org/licenses/MIT)

## Dependent libraries for core library
- [Apache Commons Math 3.3](commons.apache.org/proper/commons-math) (Apache License 2.0)
- [Apache Wink 1.4](https://wink.apache.org/) (Apache License 2.0)


## library locations
- location-service-library/lib/
 - commons-math3-3.3.jar
 - wink-1.4.jar
-----

# Sample Web API
The localization library is wrapped by an example servlet. ([You can try](#test))
This section describes about Web API that calculate location from beacon signals and motion data.

## POST /locations/beacons
### Input (post)
To localize a user carrying a smartphone, multiple pairs of beacon identifier (UUID, major id and minor id) and Received Signal Strength Indicator (RSSI) received by a smartphone at some interval are input to the library.

Data format example (JSON)

```JSON
{
  "uuid": "00000000-0000-0000-0000-000000000000",
  "data": [
    {
      "major": 1,
      "minor": 1,
      "rssi": -70
    },
    {
      "major": 1,
      "minor": 2,
      "rssi": -75
    }
  ],
  "sensorCSV": "<motion data csv as a string including new lines.>"
}
```
#### Motion data
In the case that motion data are collected in addition to beacon data, time-series data from accelerometer and gyroscope are input to the library with the beacon data.

##### Case 1: Process motion data in the library.
Motion data is input in the following format. You need to input all motion data between the last two beacon signal sampling. (i.e. iOS provide beacon data every second so you will input all motion data within a second.)

~~~
Timestamp, "Acc",  X-acceleration, Y-acceleration, Z-acceleration
Timestamp, "Motion", Pitch-angle, Roll-angle, Yaw-angle
~~~

Example (CSV)

~~~
1444834800,	Acc,	0.98407,	-0.143784,	-0.066208
1444834805,	Motion	0.185809	1.465061	-1.530919
1444834810,	Acc,	0.985413,	-0.141495,	-0.07077
1444834816,	Motion	0.186237	1.465726	-1.533538
~~~

##### Case 2: Preprocess on a smartphone
If you want to reduce amount of data transfer and do not care user's orientation, you can just specify whether a user is walking or not by processing motion data on a smartphone.

~~~
Timestamp, "Moving", Indicator if a user is moving(1) or not(0).
~~~

Example (CSV)

~~~
1444834800,	Moving,	0
1444834810,	Moving,	1
~~~



### Response
Returns user's two-dimensional location (x, y, floor).

Data format example (JSON)

~~~JSON
{"x": 1, "y": 1, "z": 0}
~~~


When user's motion is also measured, orientation is appended to the output.

~~~JSON
{"x": 1, "y": 1, "z": 0, "orientation": 1.57}
~~~

-----

# <a name="test"></a>Test Web API on your machine
Try a sample web application for BLE-based localization.
In this section, Eclipse is used to explain how to run the sample web app.

## Prerequisites
- Java Runtime Environment 7 or later
- Eclipse 4.3 or later
- WebSphere Application Server V8.5 Liberty Profile [(Download WAS Liberty in Eclipse)](https://developer.ibm.com/wasdev/downloads/liberty-profile-using-eclipse/)

## Dependent libraries for Web app
- [jQuery 1.11.2](https://jquery.com/) (MIT License)
- [OpenLayers 2.13.1](http://openlayers.org/two/) (BSD License)
- [jQuery-UI 1.11.4](https://jqueryui.com/)
- [DataTables 1.10.6](https://datatables.net/)
- [mongo-java-driver 2.12.0](https://github.com/mongodb/mongo-java-driver)

### library locations
- LocationService/WebContent/js/lib/
 - jquery/jquery-1.11.2.js
 - jquery/jquery-1.11.2.min.js
 - OpenLayers-2.13.1/
 - jquery-ui-1.11.4/
 - DataTables-1.10.6/
 
- LocationService/WebContent/WEB-INF/lib
 - mongo-java-driver-2.12.0.jar

## Launch and use the sample app
### Setup
1. Clone this repository.
- Import all projects into your Eclipse workspace.
- Put the dependent libraries by following requirements.txt in the projects.
- Launch the web application on a web server on Eclipse.

### Localization on sample data
1. Open a sample web page. It takes a while for initialization. ``http://localhost:9080/LocationService/hulo_sample.html``
2. Start localization demo by clicking the start button.

-----

# Fingerprinting data for the library
To use this library in the real world, fingerprinting i.e. collecting BLE RSSI readings at known locations is necessary.

## Data Directory
1. Create a project directory to store the prepared data as following structure and put it in 
``location-service-resource/projects/hulo/localization/resource/projects``

		/<your_project_name>
			/training
				/<any_name>.json
			/map
				/map.json
			/format.json

3. Update a setting property ``configurations.project`` in ``LocationService/resource/hulo/localization/resource/settings/deployment.json`` to ``<your_project_name>``

4. Launch the web app as previously explained.

## Fingerprint data
Fingerprint data collected by a smartphone should be stored in the following format.

Data format example (JSON)

~~~JSON
[{
	"beacons": [{
		"timestamp": 1444834000,
		"uuid": "00000000-0000-0000-0000-000000000000",
		"data": [{
			"major": 1,
			"minor": 1,
			"rssi": -77.0
		}, ...]
	}, ...],
	"information": {
		"x": 2.0,
		"y": 1.0,
		"absx": 2.0,
		"absy": 1.0,
		"floor_num": 0.0,
		"floor": 0.0
	}
}]
~~~


## Test data
Test data is used to evaluate the accuracy of the localization algorithm at given data including fingerprints. Test data consist of locations, beacon RSSI readings and motion data.

Test data format (CSV)

~~~
Timestamp, "Acc",  X-acceleration, Y-acceleration, Z-acceleration
Timestamp, "Motion", Pitch-angle, Roll-angle, Yaw-angle
Timestamp, "Beacon", X-position, Y-position, Z-position, Floor, #Beacons, Major, Minor, RSSI, ...
~~~

The third line contains a location, the number of observed beacons, and pairs of beacon identifier and RSSI.


## Beacon locations
This library uses BLE beacon positions as auxiliary information.

Data format example (GeoJSON)

~~~JSON
{
	"type": "FeatureCollection",
	"features": [{
    "type": "Feature",
    "properties": {
      "type": "beacon",
      "uuid": "00000000-0000-0000-0000-000000000000",
      "major": 1,
      "minor": 2,
		},
		"geometry": {
			"type": "Point",
			"coordinates": [1.0, 2.0]
		}
	}, ...
	]
}
~~~

-----

# Virtual fingerprinting data generator

In this sample app, an editor to create a virtual room where beacons and walls are installed is provided. The output of the editor is input to a tool to generate synthetic training and test data to be input to the localization library. The format of the data generated by these tools meets the input specifications of the library.

## Edit a virtual room
1. Open an editor to create a virtual room. ``http://localhost:9080/LocationService/env_editor.html``
- Add beacons and walls on the editor.
- Add walk path used to synthesize test data.
- Add grid used to synthesize training data.
- Export the created environment data in GeoJSON format by clicking an export button.

## Generate synthetic data
1. Write a setting JSON file to define names of files to be generated. An example is provided in location-service-library/example directory.
- Generate synthetic data by running SyntheticDataGenerator in location-service-library with arguments as follows: ``SyntheticDataGenerator -i  <PATH_TO_SETING_FILE>/<SETTING_FILE_NAME>.json``
Generated data will be saved to a directory designated by the setting file.


# Fingerprinting Management

TBD

