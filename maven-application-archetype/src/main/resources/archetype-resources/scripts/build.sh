#!/usr/bin/env bash

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd ${DIR} && cd ..

if [[ $# -gt 0 ]]; then
    PROFILE=$1
else
    PROFILE="test"
fi

if test ${PROFILE} == "dev"; then
    PROFILE="development"
elif test ${PROFILE} == "prod"; then
    PROFILE="production"
fi

mvn -e clean compile package \
    -P make-dist-exec,${PROFILE} \
    -DskipTests=true
