#!/usr/bin/env -S bash ../.port_include.sh
port='expat'
version='2.8.5'
versionpath='2_8_5'
useconfigure='true'
files=(
    "https://github.com/libexpat/libexpat/releases/download/R_${versionpath}/expat-${version}.tar.xz#1e727b8933ec51a77a9a9d9afcf8e688bce45d907c13e36ab7393fe36e703182"
)
