pwd=`pwd`

cd LocationService/WebContent/js/lib/

jquery=jquery

mkdir $jquery
cd $jquery
curl -O https://code.jquery.com/jquery-1.11.2.js
curl -O https://code.jquery.com/jquery-1.11.2.min.js

cd ..

curl -L -O https://github.com/openlayers/ol2/releases/download/release-2.13.1/OpenLayers-2.13.1.zip
unzip OpenLayers-2.13.1.zip


curl -L -O https://jqueryui.com/resources/download/jquery-ui-1.11.4.zip
unzip jquery-ui-1.11.4.zip

curl -L -O https://github.com/DataTables/DataTables/archive/1.10.6.zip
unzip 1.10.6.zip

cd $pwd
cd LocationService/WebContent/WEB-INF/lib

curl -L -O https://github.com/mongodb/mongo-java-driver/releases/download/r2.12.0/mongo-java-driver-2.12.0.jar

cd $pwd
cd location-service-library/lib

curl -L -O http://archive.apache.org/dist/commons/math/binaries/commons-math3-3.3-bin.zip
unzip commons-math3-3.3-bin.zip commons-math3-3.3/commons-math3-3.3.jar
mv commons-math3-3.3/commons-math3-3.3.jar ./

curl -L -O http://ftp.wayne.edu/apache/wink/1.4.0/apache-wink-1.4.zip
unzip apache-wink-1.4.zip apache-wink-1.4/dist/wink-1.4.jar
mv apache-wink-1.4/dist/wink-1.4.jar ./
