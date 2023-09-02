#!/usr/bin/env -S bash ../.port_include.sh
port=libogg
version=1.3.5
useconfigure=true
configopts=("--disable-static" "--enable-shared")
use_fresh_config_sub=true
files=(
    "https://github.com/xiph/ogg/releases/download/v${version}/libogg-${version}.tar.gz#0eb4b4b9420a0f51db142ba3f9c64b333f826532dc0f48c6410ae51f4799b664"
)
