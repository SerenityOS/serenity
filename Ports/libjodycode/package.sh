#!/usr/bin/env -S bash ../.port_include.sh
port='libjodycode'
version='3.1'
files=(
    "https://github.com/jbruchon/libjodycode/archive/refs/tags/v${version}.tar.gz c72974eb1d38873e06ea84b3d78990f87192f0113da5bd13fcac6bbc6a6e2184"
)
auth_type='sha256'
workdir="libjodycode-${version}"
makeopts=("UNAME_S=serenity UNAME_M=${SERENITY_ARCH} CROSS_DETECT=cross")
