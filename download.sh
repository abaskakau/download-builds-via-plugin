#!/bin/bash

buildNumberHosted=${2}
buildVersion=${3}

source properties.sh
source functions.sh

while true; do
    #Downloading Artifact
    aria2c ${ariaConfiguration} $baseURL/${1}
    #Identifying Artifact
    identifyArtifact ${1}

    mv ${1} ${artifactsStorage}/${aVersion}-${aBuildNumber}-${1}
    linkArtifact ${aVersion}-${aBuildNumber}-${1} ${aBuildNumber}
done