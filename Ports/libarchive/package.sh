#!/usr/bin/env -S bash ../.port_include.sh
port='libarchive'
version='3.7.1'
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
    "https://libarchive.org/downloads/libarchive-${version}.tar.gz#5d24e40819768f74daf846b99837fc53a3a9dcdf3ce1c2003fe0596db850f0f0"
)
depends=(
    'pcre'
    'zlib'
)

export ac_cv_header_regex_h='no'
