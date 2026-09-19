#!/usr/bin/env -S bash ../.port_include.sh
port='bzip3'
version='1.5.4'
files=(
    "https://github.com/kspalaiologos/bzip3/releases/download/${version}/bzip3-${version}.tar.gz#89a5e4bf29e4aae98b29bb1ef275addfa2d0806ba1ef60bf8a87263cdb21f581"
)
useconfigure='true'
installopts=(
    "PREFIX=${SERENITY_INSTALL_ROOT}/usr/local"
)
configopts=(
    '--disable-arch-native'
)
