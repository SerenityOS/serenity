#!/usr/bin/env -S bash ../.port_include.sh
port='libarchive'
version='3.8.10'
useconfigure='true'
configopts=(
    '--without-xml2'
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
files=(
    "https://libarchive.org/downloads/libarchive-${version}.tar.gz#a1bffbe5152cfe869751179e7b16648c87c461cb8c46eee75062efbe164cc57c"
)
depends=(
    'pcre'
    'zlib'
)

export ac_cv_header_regex_h='no'
