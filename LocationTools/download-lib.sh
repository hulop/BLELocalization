pwd=`pwd`

cd ./WebContent/js/lib/

jquery=jquery

mkdir $jquery
cd $jquery
curl -O https://code.jquery.com/jquery-1.11.3.js
curl -O https://code.jquery.com/jquery-1.11.3.min.js

cd ..

curl -L -O https://github.com/openlayers/openlayers/releases/download/v4.6.4/v4.6.4-dist.zip
unzip v4.6.4-dist.zip && rm v4.6.4-dist.zip
mv v4.6.4-dist OpenLayers-4.6.4


curl -L -O https://jqueryui.com/resources/download/jquery-ui-1.11.4.zip
unzip -o jquery-ui-1.11.4.zip && rm jquery-ui-1.11.4.zip

curl -L -O https://github.com/DataTables/DataTables/archive/1.10.10.zip
unzip -o 1.10.10.zip -x "DataTables-1.10.10/examples/**" && rm 1.10.10.zip

cd $pwd
