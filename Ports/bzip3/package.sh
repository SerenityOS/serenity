#!/usr/bin/env -S bash ../.port_include.sh
port='bzip3'
version='1.3.2'
files=(
    "https://github.com/kspalaiologos/bzip3/releases/download/${version}/bzip3-${version}.tar.gz#152cf2134fc27b68fef37d72b8c1f9f327ac611f6101d5a01287cdba24bc58c3"
)
useconfigure='true'
installopts=(
    "PREFIX=${SERENITY_INSTALL_ROOT}/usr/local"
)
configopts=(
    '--disable-arch-native'
)
