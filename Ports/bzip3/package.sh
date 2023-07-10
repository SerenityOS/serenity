#!/usr/bin/env -S bash ../.port_include.sh
port='bzip3'
version='1.2.2'
files=(
    "https://github.com/kspalaiologos/bzip3/releases/download/${version}/bzip3-${version}.tar.gz bzip3-${version}.tar.gz 19e8d379f48610f945a04a988fd0c330ff6613b3df96405d56bed35a7d216dee"
)
useconfigure='true'
installopts=("PREFIX=${SERENITY_INSTALL_ROOT}/usr/local")
configopts=(
    "--disable-arch-native"
)
