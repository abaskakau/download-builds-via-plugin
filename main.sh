#!/bin/bash

if [ "XTRIGGERCAUSE" == "$BUILD_CAUSE" ]; then
  tempFile="$HOME/.download_TEMP"
  buildVersion=`head -n1 $tempFile`
fi

echo "Triggered $buildVersion"

dataFile="$HOME/.download_${buildVersion}"
basePath="http://10.177.176.213/hosted/${buildVersion}/latest"
buildNumber=`curl $basePath/build.info | head -n1`


#Additional checks
if [ "MANUALTRIGGER" == "$BUILD_CAUSE" ]; then
  if [ "$buildNumber" == `head -n1 $dataFile` ]; then
    echo "There are no new builds"
    exit 0
  fi
fi

buildPath="`dirname $WORKSPACE`/builds/$BUILD_ID/archive"

DOWNLOAD[1]=pentaho-business-analytics-x64.exe
DOWNLOAD[2]=biserver-ee.zip
DOWNLOAD[3]=pdi-ee.zip
DOWNLOAD[4]=pentaho-business-analytics-x64.app.tar.gz
DOWNLOAD[5]=pentaho-business-analytics-x64.bin

tempLink="$PENTAHO_BUILDS_PATH/DMZ"
rm -rf $tempLink/CurrentDownload
ln -s $buildPath $tempLink/CurrentDownload


#Download
for i in $(seq ${#DOWNLOAD[@]})
do
  aria2c ${ARIA_PARAMETERS} --dir=$buildPath $basePath/${DOWNLOAD[i]}
  mv ${buildPath}/${DOWNLOAD[i]} $buildPath/$buildVersion-$buildNumber-${DOWNLOAD[i]}
done

rm -rf $tempLink/CurrentDownload

#Linking
linkPath="$PENTAHO_BUILDS_PATH/DMZ/${buildVersion}"
buildPath="`dirname $WORKSPACE`/builds/$BUILD_NUMBER/archive"
mkdir -p $linkPath
ln -s $buildPath $linkPath/${buildNumber}
rm -rf $linkPath/LATEST
ln -s $linkPath/${buildNumber} $linkPath/LATEST

#Discard old builds
deletionNumber=${buildNumber}
let "deletionNumber -= 6"
echo "The script is going to delete ${deletionNumber} and older"
for i in $(seq ${deletionNumber})
do
  rm -rf $linkPath/$i/*
  rm -rf $linkPath/$i
done

echo "${buildNumber}" > $dataFile