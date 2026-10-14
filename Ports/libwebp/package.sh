#!/usr/bin/env -S bash ../.port_include.sh
port='libwebp'
version='1.6.0'
useconfigure='true'
files=(
    "https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-${version}.tar.gz#e4ab7009bf0629fd11982d4c2aa83964cf244cffba7347ecd39019a9e38c4564"
)
depends=(
    'libjpeg'
    'libpng'
    'libtiff'
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
    "--with-pnglibdir=${SERENITY_INSTALL_ROOT}/usr/local/include"
)
