#!/usr/bin/env -S bash ../.port_include.sh
port='libarchive'
version='3.7.7'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build/autoconf/config.sub'
)
configopts=(
    '--without-xml2'
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
files=(
    "https://libarchive.org/downloads/libarchive-${version}.tar.gz#4cc540a3e9a1eebdefa1045d2e4184831100667e6d7d5b315bb1cbc951f8ddff"
)
depends=(
    'pcre'
    'zlib'
)

export ac_cv_header_regex_h='no'
