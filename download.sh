#!/bin/bash

buildNumberHosted=${2}
buildVersion=${3}

source properties.sh

function identifyArtifact {
    OUT=`egrep -a -o -m 1 "([0-9]\.){3}[0-9]-[^/]*?.jar" $1 | sed -e 's/.jar//'` #A binary search
    IFS=- read aVersion aBuildNumber <<< "${OUT}"
}
function linkArtifact {
    # 1 - artifact name; 2 - buildNumber
    mkdir -p $linkPath/${2}
    ln -s $artifactsStorage/${1} $linkPath/${2}/${1}
}
function increaseCounter {
    itc=`head -n1 it`
    let "itc+=1"
    echo $itc > it
}
function executeCommand {
    "$@"
    dStatus=$?
}
echo "I'm going to download ${1}"

while true; do
    if [[ $retries -le 0 ]]; then
        echo "[$1] Sadly but it seems i already did everything i could. Terminating the script with code 1 :("
        exit 1
    fi
    #Downloading Artifact
    echo "[$1] Starting the download"
    if [ -f "$1.aria2" ]; then
        echo "[$1] Unfinished download detected. Aria should continue the download"
    fi
    executeCommand aria2c ${ariaConfiguration} $baseURL/${1}
    if [[ $dStatus == 0 ]]; then
        echo "[$1] Download finished successfully. Status code 0. Let me try to identify what it was"
        #Identifying Artifact
        identifyArtifact ${1}
        if [[ $aBuildNumber == $buildNumberHosted ]]; then
            echo "[$1] Great. That was what we expected. Storing the file"
            mv ${1} ${artifactsStorage}/${aVersion}-${aBuildNumber}-${1}
            linkArtifact ${aVersion}-${aBuildNumber}-${1} ${aBuildNumber}
            break
        else
            echo "[$1] Unrecognized or unnecessary stuff. Burning it down and trying again"
            if [ -z "$saveFailedArtifacts" ]; then
                rm -rf ${1} ${1}.aria2
            else
                if [ -z "$aBuildNumber" ] || [ -z "$aVersion" ]; then
                    mv ${1} ${saveFailedArtifacts}/${1}
                else
                    rm -rf ${1} ${1}.aria2
                fi
            fi
        fi
    else
        echo "[$1] Download failed for some reason. Retrying"
    fi
    let "retries -= 1"
    sleep 30
done

echo "${1} just have been downloaded successfully. All tests passed. Terminating the script with code 0 :)"
increaseCounter
exit 0