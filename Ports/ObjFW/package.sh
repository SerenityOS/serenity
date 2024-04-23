#!/usr/bin/env -S bash ../.port_include.sh
port='ObjFW'
version='1.1.2'
useconfigure='true'
files=(
    "https://objfw.nil.im/downloads/objfw-${version}.tar.gz#5d9f9a70d583298e780ae11fc75a7ae2beeef904b301e1bc4f4ffa8d7ee31d9f"
)
workdir="objfw-${version}"
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
depends=(
    'openssl'
)

# Disable pledge support.
# If ObjFW detects pledge(), it expects it to be exactly OpenBSD-compatible,
# which ours is not. This then causes ObjFW to hard-abort upon trying to use
# pledge() to enter a sandbox.
configopts=('ac_cv_func_pledge=no')
