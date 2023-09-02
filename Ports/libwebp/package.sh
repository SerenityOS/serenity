#!/usr/bin/env -S bash ../.port_include.sh
port='libwebp'
version='1.3.1'
useconfigure='true'
files=(
    "https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-${version}.tar.gz#b3779627c2dfd31e3d8c4485962c2efe17785ef975e2be5c8c0c9e6cd3c4ef66"
)
depends=(
    'libjpeg'
    'libpng'
    'libtiff'
)
configopts=(
    "--with-pnglibdir=${SERENITY_INSTALL_ROOT}/usr/local/include"
)
