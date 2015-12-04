#!/bin/bash

if [ "XTRIGGERCAUSE" == "$BUILD_CAUSE" ]; then
  tempFile="$HOME/.download_TEMP"
  buildVersion=`head -n1 $tempFile`
fi

echo "Triggered $buildVersion"

source properties.sh
buildNumberHosted=`curl $baseURL/build.info | head -n1`

echo "$buildNumberHosted recieved"

#Additional checks
if [ "MANUALTRIGGER" == "$BUILD_CAUSE" ]; then
  if [[ $buildNumberHosted == $(head -n1 $dataFile) ]]; then
    echo "There are no new builds"
    exit 0
  fi
fi

#Download
echo 0 > it
itc=0
for i in $(seq ${#fileToDownload[@]})
do
    ./download.sh ${fileToDownload[i]} ${buildNumberHosted} ${buildVersion} &
    let "itc += 1"
done

echo "Waiting until all downloads will be finished (${itc})"
echo "${buildNumberHosted}" > $dataFile

while true; do
    if [[ $timeout -le 0 ]]; then
        echo "Timeout reached"
        exit 1
    fi
    if [[ $(head -n1 it) -lt $itc ]]; then
        sleep 10
        let "timeout -= 10"
    else break
  fi
done

echo "Congrats. All the files had been downloaded and identifyed. There are some messy steps left"

#Discard old builds
deletionNumber=${buildNumberHosted}
let "deletionNumber -= buildsToKeep"
echo "The script is going to delete ${deletionNumber} and older"
for i in $(seq ${deletionNumber})
do
  rm -rf `readlink -f $linkPath/${i}/*`
  rm -rf $linkPath/$i/*
  rm -rf $linkPath/$i
done