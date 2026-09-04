#!/usr/bin/env -S bash ../.port_include.sh
port='libjodycode'
version='4.1.2'
files=(
    "https://codeberg.org/jbruchon/libjodycode/archive/v${version}.tar.gz#0343cf2ff53fb19887663b8c1f47210fc2d1599a4a8f95e292991d5b1034bc05"
)
workdir='libjodycode'
makeopts=("UNAME_S=serenity UNAME_M=${SERENITY_ARCH} CROSS_DETECT=cross")
