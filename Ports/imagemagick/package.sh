#!/usr/bin/env -S bash ../.port_include.sh
port=imagemagick
version=7.1.0-61
workdir="ImageMagick-${version}"
useconfigure="true"
files="https://github.com/ImageMagick/ImageMagick/archive/refs/tags/${version}.tar.gz ${port}-v${version}.tar.gz 04a55eaf8ce093112342d777fbe0d578a5567a6bebeff0e3de0d3092fc154a06"
auth_type=sha256
configopts=("--host=${SERENITY_ARCH}")
depends=("libpng" "libtiff" "libjpeg")
