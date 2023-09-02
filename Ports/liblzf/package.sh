#!/usr/bin/env -S bash ../.port_include.sh
port='liblzf'
version='3.6'
useconfigure='true'
files=(
    "http://dist.schmorp.de/liblzf/liblzf-${version}.tar.gz#9c5de01f7b9ccae40c3f619d26a7abec9986c06c36d260c179cedd04b89fb46a"
)
configopts=(
    "--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "--exec-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
)
