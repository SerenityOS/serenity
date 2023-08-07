#!/usr/bin/env -S bash ../.port_include.sh
port=libmpg123
version=1.29.3
useconfigure=true
workdir=mpg123-${version}
use_fresh_config_sub=true
config_sub_paths=("build/config.sub")
files=(
    "https://download.sourceforge.net/project/mpg123/mpg123/${version}/mpg123-${version}.tar.bz2 963885d8cc77262f28b77187c7d189e32195e64244de2530b798ddf32183e847"
)
