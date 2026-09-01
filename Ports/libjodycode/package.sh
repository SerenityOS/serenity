#!/usr/bin/env -S bash ../.port_include.sh
port='libjodycode'
version='3.1.1'
files=(
    "https://codeberg.org/jbruchon/libjodycode/archive/v${version}.tar.gz#bc98cc2e11428585cb00cc9ceae196423abd1f6f7d49bcd2a64314490b5b8160"
)
auth_type='sha256'
workdir='libjodycode'
makeopts=("UNAME_S=serenity UNAME_M=${SERENITY_ARCH} CROSS_DETECT=cross")
