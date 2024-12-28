#!/usr/bin/env -S bash ../.port_include.sh
port='sqlite'
version='3470000'
files=(
    "https://www.sqlite.org/2024/sqlite-autoconf-${version}.tar.gz#83eb21a6f6a649f506df8bd3aab85a08f7556ceed5dbd8dea743ea003fc3a957"
)
useconfigure='true'
use_fresh_config_sub='true'
workdir="sqlite-autoconf-${version}"
