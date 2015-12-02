#!/bin/bash

if [ "XTRIGGERCAUSE" == "$BUILD_CAUSE" ]; then
  tempFile="$HOME/.download_TEMP1"
  buildVersion=`head -n1 $tempFile`
fi

echo "Triggered $buildVersion"
buildNumberHosted=`curl $basePath/build.info | head -n1`

source properties.sh

#Additional checks
if [ "MANUALTRIGGER" == "$BUILD_CAUSE" ]; then
  if [ "$buildNumberHosted" == `head -n1 $dataFile` ]; then
    echo "There are no new builds"
    exit 0
  fi
fi

#Download
for i in $(seq ${#fileToDownload[@]})
do
    ./download.sh ${fileToDownload[i]} ${buildNumberHosted} ${buildVersion} &
done

#Linking

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