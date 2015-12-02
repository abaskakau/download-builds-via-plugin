#!/bin/bash

OUT=`egrep -a -o -m 1 "([0-9]\.){3}[0-9]-[^/]*?.jar" $1 | sed -e 's/.jar//'`
IFS=- read VERSION BUILD <<< "${OUT}"

echo Version is $VERSION
echo Build number is $BUILD
