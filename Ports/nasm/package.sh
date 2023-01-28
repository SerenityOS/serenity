#!/usr/bin/env -S bash ../.port_include.sh
port=nasm
description='Netwide Assembler (NASM)'
version=2.16.01
website='https://www.nasm.us/'
files="https://www.nasm.us/pub/nasm/releasebuilds/${version}/nasm-${version}.tar.gz nasm-${version}.tar.gz d833bf0f5716e89dbcd345b7f545f25fe348c6e2ef16dbc293e1027bcd22d881"
auth_type=sha256
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("autoconf/helpers/config.sub")
makeopts=()
