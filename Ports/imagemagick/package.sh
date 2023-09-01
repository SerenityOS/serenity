#!/usr/bin/env -S bash ../.port_include.sh
port='imagemagick'
version='7.1.1-15'
workdir="ImageMagick-${version}"
useconfigure='true'
files=(
    "https://github.com/ImageMagick/ImageMagick/archive/refs/tags/${version}.tar.gz#2372192a76af9be43c0543dd7ae6dfbf34b11fc0203583453ce3f9f707c36bcc"
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
depends=(
    'libjpeg'
    'libpng'
    'libtiff'
)
