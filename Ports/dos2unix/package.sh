#!/usr/bin/env -S bash ../.port_include.sh
port=dos2unix
version=7.4.2
workdir="${port}-${version}"
files="https://waterlan.home.xs4all.nl/dos2unix/dos2unix-${version}.tar.gz ${port}-${version}.tar.gz 6035c58df6ea2832e868b599dfa0d60ad41ca3ecc8aa27822c4b7a9789d3ae01"
depends=("gettext")
auth_type=sha256
