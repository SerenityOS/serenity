#!/usr/bin/env -S bash ../.port_include.sh
port='imagemagick'
version='7.1.2-33'
workdir="ImageMagick-${version}"
useconfigure='true'
files=(
    "https://github.com/ImageMagick/ImageMagick/archive/refs/tags/${version}.tar.gz#c768ff077cccff1b682aa56653b54247b268752427ca998992b24cc7c597d9e7"
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
depends=(
    'libjpeg'
    'libpng'
    'libtiff'
)
