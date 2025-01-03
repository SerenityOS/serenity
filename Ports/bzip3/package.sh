#!/usr/bin/env -S bash ../.port_include.sh
port='bzip3'
version='1.5.1'
files=(
    "https://github.com/kspalaiologos/bzip3/releases/download/${version}/bzip3-${version}.tar.gz#cc7cacda6d15f24d3fe73fd87b895d5fd2c0f8b6dd0630ae4993aa45c4853c3b"
)
useconfigure='true'
installopts=(
    "PREFIX=${SERENITY_INSTALL_ROOT}/usr/local"
)
configopts=(
    '--disable-arch-native'
)
