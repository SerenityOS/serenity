#!/usr/bin/env -S bash ../.port_include.sh
port='freetype'
version='2.15.0'
files=(
    "https://download.savannah.gnu.org/releases/freetype/freetype-${version}.tar.gz#9186627467a616d9a9e3e574748ac0c45fa77398e6364edef3041a8ba8d92f64"
)
useconfigure='true'
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
    '--with-brotli=no'
    '--with-bzip2=no'
    '--with-zlib=no'
    '--with-harfbuzz=no'
    '--with-png=no'
)
