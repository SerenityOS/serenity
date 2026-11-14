#!/usr/bin/env -S bash ../.port_include.sh
port='lcms2'
version='2.20'
useconfigure='true'
files=(
    "https://github.com/mm2/Little-CMS/releases/download/lcms${version}/lcms2-${version}.tar.gz#18ff5504fa9cc8a2b1f3591180d90ead16c8a2ea78e530523b46eab70e1ca6b9"
)
depends=(
    'libtiff'
)
