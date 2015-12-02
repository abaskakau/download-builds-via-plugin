#!/bin/bash

#Set buildVersion and buildNumberHosted before include

dataFile="$HOME/.download_${buildVersion}1"
touch $dataFile
artifactsStorage="`dirname $WORKSPACE`/builds/$BUILD_ID/archive"
baseURL="http://10.177.176.213/hosted/${buildVersion}/latest"
ariaConfiguration="--max-tries=20 --retry-wait=30 -x 4 -s 4"
linkPath="$PENTAHO_BUILDS_PATH/DMZ1/${buildVersion}"
timeout=36000 #10 hours
retries=5

fileToDownload[1]=pentaho-business-analytics-x64.exe
fileToDownload[2]=biserver-ee.zip
#fileToDownload[3]=pdi-ee.zip
#fileToDownload[4]=pentaho-business-analytics-x64.app.tar.gz
#fileToDownload[5]=pentaho-business-analytics-x64.bin