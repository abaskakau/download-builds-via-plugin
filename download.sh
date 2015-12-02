#!/bin/bash

buildNumberHosted=${2}
buildVersion=${3}

source properties.sh

function identifyArtifact {
    OUT=`egrep -a -o -m 1 "([0-9]\.){3}[0-9]-[^/]*?.jar" $1 | sed -e 's/.jar//'`
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
    echo itc > it

}
echo "I'm going to download ${1}"

while true; do
    #Downloading Artifact
    echo "Starting the download"
    if [[ $(aria2c ${ariaConfiguration} $baseURL/${1}) == 0 ]]; then
        echo "Download finished successfully. Status code 0. Let me try to identify what it was"
        #Identifying Artifact
        identifyArtifact ${1}
        if [[ $aBuildNumber == $buildNumberHosted ]]; then
            echo "Great. That was what we expected. Storing the file"
            mv ${1} ${artifactsStorage}/${aVersion}-${aBuildNumber}-${1}
            linkArtifact ${aVersion}-${aBuildNumber}-${1} ${aBuildNumber}
            break
        else
            echo "Unrecognized or unnecessary stuff. Burning it down and trying again"
            rm -rf ${1}
        fi
    fi
done

echo "${1} just have been downloaded successfully. All tests passed. Terminating the script with code 0"
increaseCounter
exit 0