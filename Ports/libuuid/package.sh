#!/usr/bin/env -S bash ../.port_include.sh
port='libuuid'
version='2.42.4'
workdir="util-linux-${version}"
useconfigure='true'
configopts=(
    '--disable-all-programs'
    '--disable-nls'
    '--disable-static'
    '--enable-libuuid'
    '--enable-shared'
    '--prefix=/usr/local'
)
files=(
    "https://mirrors.edge.kernel.org/pub/linux/utils/util-linux/v$(cut -d. -f1,2 <<< ${version})/util-linux-${version}.tar.xz#fbd62a100ab7bb8746ba0661255c3c48185b1e9021507c624da01fbc696330ec"
)
