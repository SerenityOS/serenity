#!/usr/bin/env -S bash ../.port_include.sh

port='libuuid'
version='2.38'
workdir="util-linux-${version}"
useconfigure='true'
configopts=(
    '--disable-all-programs'
    '--disable-nls'
    '--enable-libuuid'
    '--prefix=/usr/local'
)
files=(
    "https://mirrors.edge.kernel.org/pub/linux/utils/util-linux/v${version}/util-linux-${version}.tar.gz c31d4e54f30b56b0f7ec8b342658c07de81378f2c067941c2b886da356f8ad42"
)
use_fresh_config_sub='true'
config_sub_paths=(
    'config/config.sub'
)
