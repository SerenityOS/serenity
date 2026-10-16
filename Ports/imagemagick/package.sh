#!/usr/bin/env -S bash ../.port_include.sh
port='imagemagick'
version='7.1.2-34'
workdir="ImageMagick-${version}"
useconfigure='true'
files=(
    "https://github.com/ImageMagick/ImageMagick/archive/refs/tags/${version}.tar.gz#f5163a573a1c1bff16358d0a592b2fb168ad0fd3b9dc5ff57dc7820eebc5cf79"
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
depends=(
    'libjpeg'
    'libpng'
    'libtiff'
)
