#!/usr/bin/env -S bash ../.port_include.sh
port='imagemagick'
version='7.1.2-32'
workdir="ImageMagick-${version}"
useconfigure='true'
files=(
    "https://github.com/ImageMagick/ImageMagick/archive/refs/tags/${version}.tar.gz#940e349f0ef394e658fd57400b83d1d7a81b954f6e7bdbfbfad31b2718c10add"
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
depends=(
    'libjpeg'
    'libpng'
    'libtiff'
)
