#!/usr/bin/env -S bash ../.port_include.sh
port='freetype'
version='2.14.3'
files=(
    "https://download.savannah.gnu.org/releases/freetype/freetype-${version}.tar.gz#e61b31ab26358b946e767ed7eb7f4bb2e507da1cfefeb7a8861ace7fd5c899a1"
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
