#!/bin/bash

buildNumberHosted=${2}
buildVersion=${3}

source properties.sh

function identifyArtifact {
    OUT=`egrep -a -o -m 1 "(6.1.0.0|6.0-NIGHTLY)[-]([0-9]){2}([0-9])?" $1` #A binary search
    IFS=- read aVersion aBuildNumber <<< "${OUT}"
    if [[ $aBuildNumber == *"-"* ]]; then
        IFS=- read aType aBuildNumber <<< "${aBuildNumber}"
        aVersion="${aVersion}-${aType}"
    fi
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
function renameArtifact {
    # 1 - artifactName 2 - buildVersion 3 - buildNumber
    IFS=. read aNamePart aExtensionPart <<< "${1}"
    if [[ $aNamePart == *"-x64"* ]]; then
        aNamePart=`echo $aNamePart | sed -e 's/-x64//'`
        aNewName="${aNamePart}-${2}-${3}-x64.${aExtensionPart}"
    else
        aNewName="${aNamePart}-${2}-${3}.${aExtensionPart}"
    fi
}
function renameArtifact54NIGHTLY {
    # 1 - artifactName 2 - buildVersion
    IFS=. read aNamePart aExtensionPart <<< "${1}"
    if [[ $aNamePart == *"-x64"* ]]; then
        aNamePart=`echo $aNamePart | sed -e 's/-x64//'`
        aNewName="${aNamePart}-${2}-x64.${aExtensionPart}"
    else
        aNewName="${aNamePart}-${2}.${aExtensionPart}"
    fi
}
echo "I'm going to download ${1}"

if [[ $buildVersion == "5.4-NIGHTLY" ]]; then
    echo "Executing special download mechanism for 5.4-NIGHTLY"
    while true; do
        if [[ $retries -le 1 ]]; then
            renameArtifact54NIGHTLY ${1} ${buildVersion}
            executeCommand aria2c ${ariaConfiguration} ftp://ftp.box.com/CI/${buildVersion}/${buildNumberHosted}/${aNewName}
            if [[ $dStatus == 0 ]]; then
                mv ${aNewName} ${artifactsStorage}/${aNewName}
                linkArtifact ${aNewName} ${buildNumberHosted}
                increaseCounter
                exit 0
            else
                exit 1
            fi
        fi
        renameArtifact54NIGHTLY ${1} ${buildVersion}
        executeCommand aria2c ${ariaConfiguration} ftp://ftp.box.com/CI/${buildVersion}/${buildNumberHosted}/${aNewName} $baseURL/${1}
        if [[ $dStatus == 0 ]]; then
            mv ${aNewName} ${artifactsStorage}/${aNewName}
            linkArtifact ${aNewName} ${buildNumberHosted}
            increaseCounter
            exit 0
        fi
        let "retries -= 1"
        sleep 30
    done
fi

if [[ $buildVersion == "6.0-NIGHTLY" ]]; then
    echo "Waiting while build will be uploaded"
    sleep 1800
fi

while true; do
    if [[ $retries -le 1 ]]; then
        echo "The Last Attempt. I'm going to get this file from the Box"
        renameArtifact ${1} ${buildVersion} ${buildNumberHosted}
        executeCommand aria2c ${ariaConfiguration} ftp://ftp.box.com/CI/${buildVersion}/${buildNumberHosted}/${aNewName}
        if [[ $dStatus == 0 ]]; then
            echo "That was another time when pentaho gave us some crap we should sort. Anyway we got the file from the alternative storage"
            mv ${1} ${artifactsStorage}/${aNewName}
            linkArtifact ${aNewName} ${buildNumberHosted}
            break
        else
            echo "[$1] Sadly but it seems i already did everything i could. Terminating the script with code 1 :("
            exit 1
        fi
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
            renameArtifact ${1} ${aVersion} ${aBuildNumber}
            mv ${1} ${artifactsStorage}/${aNewName}
            linkArtifact ${aNewName} ${aBuildNumber}
            break
        else
            echo "[$1] Unrecognized or unnecessary stuff. Burning it down and trying again"
            if [ -z "$saveFailedArtifacts" ]; then
                rm -rf ${1} ${1}.aria2
            else
                if [ -z "$aBuildNumber" ] || [ -z "$aVersion" ]; then
                    echo "[CRITICAL] Script parse error!!!"
                    mkdir -p $saveFailedArtifacts
                    mv ${1} ${saveFailedArtifacts}/${1}
                    exit 1
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