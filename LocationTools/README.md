<!--
The MIT License (MIT)

Copyright (c) 2014, 2023 IBM Corporation
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
# LocationTools

## About
[About HULOP](https://github.com/hulop/00Readme)

## License
[MIT](http://opensource.org/licenses/MIT)

## Prerequisites
- Java Runtime Environment 7 or later
- Eclipse 4.3 or later
- WebSphere Application Server V8.5 Liberty Profile [(Download WAS Liberty in Eclipse)](https://developer.ibm.com/wasdev/downloads/liberty-profile-using-eclipse/)

## Dependent libraries
- [jQuery 1.11.3](https://jquery.com/) (MIT License)
- [OpenLayers 2.13.1](http://openlayers.org/two/) (BSD License)
- [jQuery-UI 1.11.4](https://jqueryui.com/) (MIT License)
- [DataTables 1.10.10](https://datatables.net/) (MIT License)
- [mongo-java-driver 3.4.0](https://github.com/mongodb/mongo-java-driver) (Apache License 2.0)

### library locations
- LocationService/WebContent/js/lib/
  - jquery/jquery-1.11.3.js
  - jquery/jquery-1.11.3.min.js
  - OpenLayers-2.13.1/
  - jquery-ui-1.11.4/
  - DataTables-1.10.10/
 
- LocationService/WebContent/WEB-INF/lib
  - mongo-java-driver-3.4.0.jar


### Setup
1. Download libraries
    ```
    $ bash ./download-lib.sh
    ```

2. Prepare mongodb
    ```
    $  docker compose -f docker-compose-mongo.yaml up mongodb
    ```
    example of docker-compose-mongo.yaml
    ```
    version: "2.3"

    services:
      mongodb:
        image : mongo:3.4.3
        environment:
          - PUID=1000
          - PGID=1000
        volumes:
          - ./mongodb/database:/data/db
        ports:
          - 127.0.0.1:27017:27017  # expose db to localhost
        networks:
          - mongodb_network
        healthcheck:
          test: echo 'db.runCommand("ping").ok' | mongo mongodb:27017/test --quiet
          interval: 3s
          timeout: 3s
          retries: 5

    networks:
      mongodb_network:
        name: mongodb_network
    ```

3. Launch the server app

4. Login as admin

5. Create a new database

    ![Screenshot from 2023-12-19 01-50-28](https://github.com/CMU-cabot/TODO-Consortium/assets/141628678/f40d0410-5fa4-4552-bfbc-f330dd0503e2)

6. Click the created database and add a floorplan

    ![Screenshot from 2023-12-19 01-57-00](https://github.com/CMU-cabot/TODO-Consortium/assets/141628678/4b307123-0526-4982-b5d9-8ad49adfa20f)