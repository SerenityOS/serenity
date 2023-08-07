#!/usr/bin/env -S bash ../.port_include.sh
port=nasm
version=2.16.01
files=(
    "https://www.nasm.us/pub/nasm/releasebuilds/${version}/nasm-${version}.tar.gz d833bf0f5716e89dbcd345b7f545f25fe348c6e2ef16dbc293e1027bcd22d881"
)
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("autoconf/helpers/config.sub")
makeopts=()
