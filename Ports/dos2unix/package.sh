#!/usr/bin/env -S bash ../.port_include.sh
port=dos2unix
version=7.4.2
workdir="${port}-${version}"
files="https://waterlan.home.xs4all.nl/dos2unix/dos2unix-${version}.tar.gz ${port}-${version}.tar.gz"
depends=("gettext")
