#!/usr/bin/env -S bash ../.port_include.sh
port='libjodycode'
version='4.2'
files=(
    "https://codeberg.org/jbruchon/libjodycode/archive/v${version}.tar.gz#eeb54fc50b9ebec4e43c954704e69f56c104fede67aaee9f375fbecbb87933b0"
)
workdir='libjodycode'
makeopts=("UNAME_S=serenity UNAME_M=${SERENITY_ARCH} CROSS_DETECT=cross")
