#!/usr/bin/env bash

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd $script_path

pushd gcc
    ./package.sh
popd

pushd libstdc++-v3
    ./package.sh
popd