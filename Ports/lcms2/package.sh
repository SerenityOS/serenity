#!/usr/bin/env -S bash ../.port_include.sh
port='lcms2'
version='2.15'
useconfigure='true'
use_fresh_config_sub='true'
use_fresh_config_guess='true'
files=(
    "https://github.com/mm2/Little-CMS/releases/download/lcms${version}/lcms2-${version}.tar.gz b20cbcbd0f503433be2a4e81462106fa61050a35074dc24a4e356792d971ab39"
)
depends=(
    'libtiff'
)
