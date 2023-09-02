#!/usr/bin/env -S bash ../.port_include.sh
port=dos2unix
version=7.5.1
workdir="${port}-${version}"
files=(
    "https://waterlan.home.xs4all.nl/dos2unix/dos2unix-${version}.tar.gz#da07788bb2e029b0d63f6471d166f68528acd8da2cf14823a188e8a9d5c1fc15"
)
depends=("gettext")
