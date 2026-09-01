#!/usr/bin/env -S bash ../.port_include.sh
port='lcms2'
version='2.19.1'
useconfigure='true'
files=(
    "https://github.com/mm2/Little-CMS/releases/download/lcms${version}/lcms2-${version}.tar.gz#bfc54f7bab59fbc921012014a8032e4cba4abd46db47d46b76416a8c0b2815c8"
)
depends=(
    'libtiff'
)
