#!/usr/bin/env -S bash ../.port_include.sh
port='imagemagick'
version='7.1.2-30'
workdir="ImageMagick-${version}"
useconfigure='true'
files=(
    "https://github.com/ImageMagick/ImageMagick/archive/refs/tags/${version}.tar.gz#3034a64f22398e15ee3dd1e6b1aa83d838cfc47df1bb246ae0eca9590e6ace72"
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
depends=(
    'libjpeg'
    'libpng'
    'libtiff'
)
