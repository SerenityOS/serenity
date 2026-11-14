#!/usr/bin/env -S bash ../.port_include.sh
port='imagemagick'
version='7.1.2-35'
workdir="ImageMagick-${version}"
useconfigure='true'
files=(
    "https://github.com/ImageMagick/ImageMagick/archive/refs/tags/${version}.tar.gz#40b6fefa88fe16ca5a4b3d3db5133eaebfb0157f21f98530bde8aec5dd5b0776"
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
depends=(
    'libjpeg'
    'libpng'
    'libtiff'
)
