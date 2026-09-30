#!/usr/bin/env -S bash ../.port_include.sh
port='sed'
version='4.10'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
files=(
    "mirror://gnu/sed/sed-${version}.tar.gz#4d179ffaf92ec4dcec541f7c032be1c3b9a1856f4970adb95a505221702f5277"
)
