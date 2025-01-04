#!/usr/bin/env -S bash ../.port_include.sh
port=nasm
version=2.16.03
files=(
    "https://www.nasm.us/pub/nasm/releasebuilds/${version}/nasm-${version}.tar.gz#5bc940dd8a4245686976a8f7e96ba9340a0915f2d5b88356874890e207bdb581"
)
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("autoconf/helpers/config.sub")
makeopts=()
