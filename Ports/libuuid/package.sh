#!/usr/bin/env -S bash ../.port_include.sh
port='libuuid'
version='2.39.2'
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
    "https://mirrors.edge.kernel.org/pub/linux/utils/util-linux/v$(cut -d. -f1,2 <<< ${version})/util-linux-${version}.tar.xz#87abdfaa8e490f8be6dde976f7c80b9b5ff9f301e1b67e3899e1f05a59a1531f"
)
use_fresh_config_sub='true'
config_sub_paths=(
    'config/config.sub'
)
