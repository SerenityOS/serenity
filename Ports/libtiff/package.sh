#!/usr/bin/env -S bash ../.port_include.sh
port='libtiff'
version='4.5.0'
files=(
    "http://download.osgeo.org/libtiff/tiff-${version}.tar.xz dafac979c5e7b6c650025569c5a4e720995ba5f17bc17e6276d1f12427be267c"
)
useconfigure='true'
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
    "--prefix=/usr/local"
    "--disable-static"
    "--enable-shared"
)
workdir="tiff-${version}"
depends=("libjpeg" "zstd" "xz")
