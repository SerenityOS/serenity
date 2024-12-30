#!/usr/bin/env -S bash ../.port_include.sh
port='libjodycode'
version='3.1.1'
files=(
    "https://codeberg.org/jbruchon/libjodycode/archive/v${version}.tar.gz#82717625b91f92ea4e7435798385762c6906835fa2cf2c80c9f83d6734bfddad"
)
auth_type='sha256'
workdir='libjodycode'
makeopts=("UNAME_S=serenity UNAME_M=${SERENITY_ARCH} CROSS_DETECT=cross")
