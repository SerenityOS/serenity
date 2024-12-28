#!/usr/bin/env -S bash ../.port_include.sh
port='ObjFW'
version='1.2.2'
useconfigure='true'
files=(
    "https://objfw.nil.im/downloads/objfw-${version}.tar.gz#4fe0bed1ec21561a184d804aa577ff630f1e3d20b1c3b973073e23ce829294a1"
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
