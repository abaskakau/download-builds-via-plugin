#!/bin/bash

tempFile="$HOME/.download_TEMP"
buildVersion=`head -n1 $tempFile`

echo "[MAIN] Triggered $buildVersion"

source properties.sh
buildNumberHosted=`curl $baseURL/build.info | head -n1`

echo "$buildNumberHosted recieved"

#Additional checks
#if [ "MANUALTRIGGER" == "$BUILD_CAUSE" ]; then
#  if [[ $buildNumberHosted == $(head -n1 $dataFile) ]]; then
#    echo "[MAIN] There are no new builds"
#    exit 0
#  fi
#fi

if [ "$buildVersion" == "6.1-QAT" ]; then
    echo "Waiting 15 additional minutes for 6.1"
    sleep 900
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
    notify-send -i /usr/share/icons/Adwaita/32x32/status/dialog-error.png "Build Download Failure" "`tac /home/dbuilds/.downl.log | head -n50`"
    exit 1
else
    echo "[MAIN] Congrats. All the files had been downloaded and identifyed. There are some messy steps left"
    notify-send -i /usr/share/icons/Adwaita/32x32/status/network-transmit.png "Build Downloaded" "Build has been downloaded successfuly"
fi

#Discard old builds
deletionNumber=${buildNumberHosted}
let "deletionNumber -= buildsToKeep"
echo "[MAIN] The script is going to delete ${deletionNumber} and older"
#for i in $(seq ${deletionNumber})
#do
#  rm -rf `readlink -f $linkPath/${i}/*`
#  rm -rf $linkPath/$i/*
#  rm -rf $linkPath/$i
#done
echo "[MAIN][WARNING] Deleting temporary disabled"

exit 0