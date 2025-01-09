#!/usr/bin/env -S bash ../.port_include.sh
port='lcms2'
version='2.16'
useconfigure='true'
use_fresh_config_sub='true'
use_fresh_config_guess='true'
files=(
    "https://github.com/mm2/Little-CMS/releases/download/lcms${version}/lcms2-${version}.tar.gz#d873d34ad8b9b4cea010631f1a6228d2087475e4dc5e763eb81acc23d9d45a51"
)
depends=(
    'libtiff'
)
