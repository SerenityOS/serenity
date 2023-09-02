#!/usr/bin/env -S bash ../.port_include.sh
port=libexpat
version=2.4.8
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("conftools/config.sub")
files=(
    "https://github.com/libexpat/libexpat/releases/download/R_${version//./_}/expat-${version}.tar.xz#f79b8f904b749e3e0d20afeadecf8249c55b2e32d4ebb089ae378df479dcaf25"
)
workdir=expat-${version}
