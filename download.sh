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

echo "I'm going to download ${1}"

while true; do
    #Downloading Artifact
    aria2c ${ariaConfiguration} $baseURL/${1}
    #Identifying Artifact
    identifyArtifact ${1}

    mv ${1} ${artifactsStorage}/${aVersion}-${aBuildNumber}-${1}
    linkArtifact ${aVersion}-${aBuildNumber}-${1} ${aBuildNumber}
done