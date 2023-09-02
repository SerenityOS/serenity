#!/usr/bin/env -S bash ../.port_include.sh
port=sparsehash
version=2.0.4
useconfigure=true
files=(
    "https://github.com/sparsehash/sparsehash/archive/refs/tags/$port-$version.tar.gz#8cd1a95827dfd8270927894eb77f62b4087735cbede953884647f16c521c7e58"
)
workdir=$port-$port-$version
