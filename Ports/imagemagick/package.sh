#!/usr/bin/env -S bash ../.port_include.sh
port='imagemagick'
version='7.1.2-31'
workdir="ImageMagick-${version}"
useconfigure='true'
files=(
    "https://github.com/ImageMagick/ImageMagick/archive/refs/tags/${version}.tar.gz#34d9cc3acddc3e3c429d23af60eda5ceaac477a8b296ddb9469f773f44a80a5f"
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
depends=(
    'libjpeg'
    'libpng'
    'libtiff'
)

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"
