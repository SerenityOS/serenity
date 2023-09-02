#!/usr/bin/env -S bash ../.port_include.sh
port=libjpeg
version=9e
useconfigure=true
configopts=("--disable-static" "--enable-shared")
files=(
    "https://ijg.org/files/jpegsrc.v${version}.tar.gz#4077d6a6a75aeb01884f708919d25934c93305e49f7e3f36db9129320e6f4f3d"
)
workdir="jpeg-$version"
