#!/usr/bin/env -S bash ../.port_include.sh
port='libuuid'
version='2.42.3'
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
    "https://mirrors.edge.kernel.org/pub/linux/utils/util-linux/v$(cut -d. -f1,2 <<< ${version})/util-linux-${version}.tar.xz#66ac7c0e725278eb2b039e3104f2c91119341d941b41bac7a285c695f940bd57"
)
