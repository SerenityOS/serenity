#!/usr/bin/env -S bash ../.port_include.sh
port='bzip3'
version='1.5.5'
files=(
    "https://github.com/kspalaiologos/bzip3/releases/download/${version}/bzip3-${version}.tar.gz#dc3bd8237178e1a2b99d8dcf1f4aeb3593c723788a9863606e121b40784e596d"
)
useconfigure='true'
installopts=(
    "PREFIX=${SERENITY_INSTALL_ROOT}/usr/local"
)
configopts=(
    '--disable-arch-native'
)
