#!/usr/bin/env -S bash ../.port_include.sh
port=dos2unix
version=7.5.0
workdir="${port}-${version}"
files=(
    "https://waterlan.home.xs4all.nl/dos2unix/dos2unix-${version}.tar.gz ${port}-${version}.tar.gz 7a3b01d01e214d62c2b3e04c3a92e0ddc728a385566e4c0356efa66fd6eb95af"
)
depends=("gettext")
