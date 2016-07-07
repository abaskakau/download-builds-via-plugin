#!/bin/bash

if pidof -s aria2c > /dev/null; then
    exit 0
fi

buildVersions[1]="7.0-QAT"
buildVersions[2]="6.1-QAT"
buildVersions[3]="5.4-NIGHTLY"

for i in $(seq ${#buildVersions[@]})
do
  #Getting the latest build number
  basePath="http://10.177.176.213/hosted/${buildVersions[i]}/latest"
  buildNumberAct=`curl $basePath/build.info | head -n1`
  echo "$buildNumberAct latest for ${buildVersions[i]}"

  #Checking the latest downloaded build
  dataFile="$HOME/.download_${buildVersions[i]}"
  touch $dataFile
  buildNumberLast=`head -n1 $dataFile`
  if [ "$buildNumberLast" == "" ]; then
    buildNumberLast=0
  fi
  echo "buildNumberlast=$buildNumberLast"

  #Triggering the build
  if [ "$buildNumberLast" -lt "$buildNumberAct" ]; then
    tempFile="$HOME/.download_TEMP"
    echo ${buildVersions[i]} > $tempFile
    echo "Triggering build for ${buildVersions[i]}-${buildNumberAct}"
    ./main.sh &
    exit 0
  fi
done