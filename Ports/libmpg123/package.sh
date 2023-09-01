#!/usr/bin/env -S bash ../.port_include.sh
port='libmpg123'
version='1.31.3'
useconfigure='true'
workdir="mpg123-${version}"
use_fresh_config_sub='true'
config_sub_paths=(
    'build/config.sub'
)
files=(
    "https://download.sourceforge.net/project/mpg123/mpg123/${version}/mpg123-${version}.tar.bz2#1ca77d3a69a5ff845b7a0536f783fee554e1041139a6b978f6afe14f5814ad1a"
)
