#!/usr/bin/env bash

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd $script_path

pushd qemu
    ./package.sh
popd

pushd python
    ./package.sh
popd

pushd fuse-ext2
    ./package.sh
popd
