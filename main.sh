#!/bin/bash

if [ "XTRIGGERCAUSE" == "$BUILD_CAUSE" ]; then
  tempFile="$HOME/.download_TEMP1"
  buildVersion=`head -n1 $tempFile`
fi

echo "Triggered $buildVersion"

source properties.sh
buildNumberHosted=`curl $baseURL/build.info | head -n1`

#Additional checks
if [ "MANUALTRIGGER" == "$BUILD_CAUSE" ]; then
  if [ "$buildNumberHosted" == `head -n1 $dataFile` ]; then
    echo "There are no new builds"
    exit 0
  fi
fi

#Download
touch it
itc = 0
for i in $(seq ${#fileToDownload[@]})
do
    ./download.sh ${fileToDownload[i]} ${buildNumberHosted} ${buildVersion} &
    let "itc += 1"
done

while true; do
    if [[ $(head -n1 it) -lt $itc ]]; then
        sleep 10
    else break
  fi
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