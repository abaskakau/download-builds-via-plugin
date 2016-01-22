#!/bin/bash

if [ "XTRIGGERCAUSE" == "$BUILD_CAUSE" ]; then
  tempFile="$HOME/.download_TEMP"
  buildVersion=`head -n1 $tempFile`
fi

echo "[MAIN] Triggered $buildVersion"

source properties.sh
buildNumberHosted=`curl $baseURL/build.info | head -n1`

echo "$buildNumberHosted recieved"

#Additional checks
if [ "MANUALTRIGGER" == "$BUILD_CAUSE" ]; then
  if [[ $buildNumberHosted == $(head -n1 $dataFile) ]]; then
    echo "[MAIN] There are no new builds"
    exit 0
  fi
fi

#Download
echo 0 > it
echo 0 > itf
itc=0
itfc=0
for i in $(seq ${#fileToDownload[@]})
do
    ./download.sh ${fileToDownload[i]} ${buildNumberHosted} ${buildVersion} &
    let "itc += 1"
done

echo "[MAIN] Waiting until all downloads will be finished (${itc})"
echo "${buildNumberHosted}" > $dataFile

if [[ $buildVersion == "6.0-NIGHTLY" ]]; then
    echo "[MAIN] Timeout increased for 6.0-NIGHTLY"
    let "timeout += 3600"
fi

while true; do
    if [[ $timeout -le 0 ]]; then
        echo "[MAIN] Timeout reached"
        exit 1
    fi
    suJobs=`head -n1 it`
    faJobs=`head -n1 itf`
    fiJobs=suJobs
    let "fiJobs += faJobs"
    if [[ $fiJobs -lt $itc ]]; then
        sleep 10
        let "timeout -= 10"
    else break
  fi
done

if [[ $faJobs -gt 0 ]]; then
    echo "[MAIN] One or more jobs had been failed. Terminating the main script"
    exit 1
else
    echo "[MAIN] Congrats. All the files had been downloaded and identifyed. There are some messy steps left"
fi

#Discard old builds
deletionNumber=${buildNumberHosted}
let "deletionNumber -= buildsToKeep"
echo "[MAIN] The script is going to delete ${deletionNumber} and older"
for i in $(seq ${deletionNumber})
do
  rm -rf `readlink -f $linkPath/${i}/*`
  rm -rf $linkPath/$i/*
  rm -rf $linkPath/$i
done