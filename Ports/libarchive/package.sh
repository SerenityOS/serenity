#!/usr/bin/env -S bash ../.port_include.sh
port='libarchive'
version='3.8.11'
useconfigure='true'
configopts=(
    '--without-xml2'
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
files=(
    "https://libarchive.org/downloads/libarchive-${version}.tar.gz#900fe9fc7237c2b04f090f3c3648de8fe597525771f5035016537ce2d667d12b"
)
depends=(
    'pcre'
    'zlib'
)

export ac_cv_header_regex_h='no'
