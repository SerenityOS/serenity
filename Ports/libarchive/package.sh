#!/usr/bin/env -S bash ../.port_include.sh
port='libarchive'
version='3.8.9'
useconfigure='true'
configopts=(
    '--without-xml2'
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
files=(
    "https://libarchive.org/downloads/libarchive-${version}.tar.gz#f5a6539059cf5e597dbeda37bfa4874b1e8dea063c8d93bf85a2b44af90a5bd4"
)
depends=(
    'pcre'
    'zlib'
)

export ac_cv_header_regex_h='no'
