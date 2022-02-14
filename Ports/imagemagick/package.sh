#!/usr/bin/env -S bash ../.port_include.sh
port=ImageMagick
version=7.1.0-23
workdir="${port}-${version}"
useconfigure="true"
files="https://github.com/ImageMagick/ImageMagick/archive/refs/tags/${version}.tar.gz imagemagick-v${version}.tar.gz 62c24362891d0af2be9a81d01117195ba0ec8e6982c7568195a33019bfc82188"
auth_type=sha256
configopts=("--host=${SERENITY_ARCH}")
depends=("libpng" "libtiff" "libjpeg")
