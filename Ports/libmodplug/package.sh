#!/usr/bin/env -S bash ../.port_include.sh
port='libmodplug'
version='0.8.9.2'
useconfigure='true'
configopts=("ac_cv_c_bigendian=no")
files=(
    "https://download.sourceforge.net/modplug-xmms/libmodplug-${version}.tar.gz#e4d9cb46da78a639d6eadb4ff3ab955f68c3099579b5310af64b37091e28957e"
)
