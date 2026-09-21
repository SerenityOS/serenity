#!/usr/bin/env -S bash ../.port_include.sh
port='libjpeg'
version='10'
useconfigure=true
configopts=("--disable-static" "--enable-shared")
files=(
    "https://ijg.org/files/jpegsrc.v${version}.tar.gz#8b9eaa13242690ebd03e1728ab1edf97a81a78ed6e83624d493655f31ac95ab5"
)
workdir="jpeg-$version"
