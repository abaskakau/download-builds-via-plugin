#!/bin/bash

buildVersions[1]="6.1.0.0"
buildVersions[2]="6.0.1.0"
buildVersions[3]="6.0.0.0"
buildVersions[4]="5.4.0.7"
buildVersions[5]="MASTER-NIGHTLY"
buildVersions[6]="6.0-NIGHTLY"
buildVersions[7]="5.4-NIGHTLY"

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
    exit 0
  fi
done

exit 1