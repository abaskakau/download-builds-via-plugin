#!/bin/bash

#Set buildVersion and buildNumberHosted before include

dataFile="$HOME/.download_${buildVersion}"
touch $dataFile
artifactsStorage="`dirname $WORKSPACE`/builds/$BUILD_ID/archive"
mkdir -p $artifactsStorage
baseURL="http://10.177.176.213/hosted/${buildVersion}/latest"
ariaConfiguration="--max-tries=20 --retry-wait=30 -x 5 -s 5"
linkPath="$PENTAHO_BUILDS_PATH/DMZ/${buildVersion}"
timeout=21600 #6 hours in seconds
retries=5
buildsToKeep=4
saveFailedArtifacts="${linkPath}/${buildNumberHosted}/burned" #Comment this line to delete all wrong files

fileToDownload[1]=pentaho-business-analytics-x64.exe
fileToDownload[2]=biserver-ee.zip
fileToDownload[3]=pdi-ee.zip
fileToDownload[4]=pentaho-business-analytics-x64.app.tar.gz
fileToDownload[5]=pentaho-business-analytics-x64.bin