#!/usr/bin/env bash
if [ -z $1 ]; then echo "you must provide a version number"; exit 1; fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

gsed -i 's/libVersionName = ".*"/libVersionName = "'$1'"/g' $DIR/build.gradle
gsed -i 's/libVersionName = ".*"/libVersionName = "'$1'"/g' $DIR/primedioandroid/build.gradle
gsed -i 's/libraryVersion = ".*"/libraryVersion = "'$1'"/g' $DIR/primedioandroid/build.gradle
gsed -i 's/        String sdkVersion = ".*";/        String sdkVersion = "'$1'";/g' $DIR/primedioandroid/src/main/java/io/primed/primedandroid/PrimedTracker.java