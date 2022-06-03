#!/usr/bin/env -S bash ../.port_include.sh
port=libtiff
version=4.3.0
useconfigure=true
configopts=("--disable-static" "--enable-shared")
workdir="tiff-$version"
files="http://download.osgeo.org/libtiff/tiff-${version}.tar.gz tiff-${version}.tar.gz 0e46e5acb087ce7d1ac53cf4f56a09b221537fc86dfc5daaad1c2e89e1b37ac8"
auth_type="sha256"
depends=("libjpeg" "zstd" "xz")
