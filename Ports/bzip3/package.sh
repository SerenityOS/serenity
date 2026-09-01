#!/usr/bin/env -S bash ../.port_include.sh
port='bzip3'
version='1.5.3'
files=(
    "https://github.com/kspalaiologos/bzip3/releases/download/${version}/bzip3-${version}.tar.gz#c48823353084df2a5a0dba44fd5295abd078e40b49f09700d08af4d9b1e31d67"
)
useconfigure='true'
installopts=(
    "PREFIX=${SERENITY_INSTALL_ROOT}/usr/local"
)
configopts=(
    '--disable-arch-native'
)
