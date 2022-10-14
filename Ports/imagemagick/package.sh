#!/usr/bin/env -S bash ../.port_include.sh
port=imagemagick
version=7.1.0-29
workdir="ImageMagick-${version}"
useconfigure="true"
files="https://github.com/ImageMagick/ImageMagick/archive/refs/tags/${version}.tar.gz ${port}-v${version}.tar.gz 889be185fd17a9b9b3d4090e28aecdec289a4f690964a7964b4f893c7a8ec21c"
auth_type=sha256
configopts=("--host=${SERENITY_ARCH}")
depends=("libpng" "libtiff" "libjpeg")
