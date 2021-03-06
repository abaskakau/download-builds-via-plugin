#!/bin/bash

#Set buildVersion and buildNumberHosted before include

dataFile="$HOME/.download_${buildVersion}"
touch $dataFile
artifactsStorage="$HOME/Artifacts"
mkdir -p $artifactsStorage
baseURL="http://10.177.176.213/hosted/${buildVersion}/latest"
ariaConfiguration="--max-tries=20 --retry-wait=30 -x 5 -s 5"
linkPath="$HOME/DMZ/${buildVersion}"
timeout=21600 #6 hours in seconds
retries=4 #The last one will be from the box over ftp. It may be slow
buildsToKeep=4
#saveFailedArtifacts="${linkPath}/${buildNumberHosted}/resurrected" #Comment this line to delete all wrong files

fileToDownload[1]=pentaho-business-analytics-x64.exe
fileToDownload[2]=biserver-ee.zip
fileToDownload[3]=pdi-ee-client.zip
fileToDownload[4]=pentaho-business-analytics-x64.app.tar.gz
fileToDownload[5]=pentaho-business-analytics-x64.bin