#!/usr/bin/env -S bash ../.port_include.sh
port='imagemagick'
version='7.1.1-5'
workdir="ImageMagick-${version}"
useconfigure="true"
files=(
    "https://github.com/ImageMagick/ImageMagick/archive/refs/tags/${version}.tar.gz#dd23689304b8cf41572c3af6b0ddccfe21c5b9d9abddaf978f314696408d0750"
)
configopts=("--with-sysroot=${SERENITY_INSTALL_ROOT}")
depends=("libpng" "libtiff" "libjpeg")
