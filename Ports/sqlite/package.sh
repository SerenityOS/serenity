#!/usr/bin/env -S bash ../.port_include.sh
port='sqlite'
version='3460000'
files=(
    "https://www.sqlite.org/2024/sqlite-autoconf-${version}.tar.gz#6f8e6a7b335273748816f9b3b62bbdc372a889de8782d7f048c653a447417a7d"
)
useconfigure='true'
use_fresh_config_sub='true'
workdir="sqlite-autoconf-${version}"
