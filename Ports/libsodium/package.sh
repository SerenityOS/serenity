#!/usr/bin/env -S bash ../.port_include.sh
port=libsodium
version=1.0.18
useconfigure=true
configopts=("--disable-static" "--enable-shared")
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
workdir=libsodium-${version}
files=(
    "https://download.libsodium.org/libsodium/releases/libsodium-${version}.tar.gz#6f504490b342a4f8a4c4a02fc9b866cbef8622d5df4e5452b46be121e46636c1"
)
