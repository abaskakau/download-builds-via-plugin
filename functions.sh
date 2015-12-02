#!/bin/bash

function identifyArtifact {
    OUT=`egrep -a -o -m 1 "([0-9]\.){3}[0-9]-[^/]*?.jar" $1 | sed -e 's/.jar//'`
    IFS=- read aVersion aBuildNumber <<< "${OUT}"
}
function linkArtifact {
    # 1 - artifact name; 2 - buildNumber
    mkdir -p $linkPath/${2}
    ln -s $artifactsStorage/${1} $linkPath/${2}/${1}
}