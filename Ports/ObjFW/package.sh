#!/usr/bin/env -S bash ../.port_include.sh
port='ObjFW'
version='1.5.7'
useconfigure='true'
files=(
    "https://objfw.nil.im/downloads/objfw-${version}.tar.gz#e637c32731dc07396b812c4019f34d1417a3f7aa39d450b7f27c9bcdc23b3e12"
)
workdir="objfw-${version}"
depends=(
    'openssl'
)

# Disable pledge support.
# If ObjFW detects pledge(), it expects it to be exactly OpenBSD-compatible,
# which ours is not. This then causes ObjFW to hard-abort upon trying to use
# pledge() to enter a sandbox.
configopts=('ac_cv_func_pledge=no')
