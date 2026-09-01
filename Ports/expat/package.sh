#!/usr/bin/env -S bash ../.port_include.sh
port='expat'
version='2.8.4'
versionpath='2_8_4'
useconfigure='true'
files=(
    "https://github.com/libexpat/libexpat/releases/download/R_${versionpath}/expat-${version}.tar.xz#656ae1cc8da3b4ea513bb4e254f33e6243938084c0ec6239da873376b09985a7"
)
