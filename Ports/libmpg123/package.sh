#!/usr/bin/env -S bash ../.port_include.sh
port='libmpg123'
version='1.33.4'
useconfigure='true'
workdir="mpg123-${version}"
use_fresh_config_sub='true'
config_sub_paths=(
    'build/config.sub'
)
files=(
    "https://download.sourceforge.net/project/mpg123/mpg123/${version}/mpg123-${version}.tar.bz2#3ae8c9ff80a97bfc0e22e89fbcd74687eca4fc1db315b12607f27f01cb5a47d9"
)
