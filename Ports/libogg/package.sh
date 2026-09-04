#!/usr/bin/env -S bash ../.port_include.sh
port='libogg'
version='1.3.6'
useconfigure='true'
configopts=("--disable-static" "--enable-shared")
files=(
    "https://github.com/xiph/ogg/releases/download/v${version}/libogg-${version}.tar.gz#83e6704730683d004d20e21b8f7f55dcb3383cdf84c0daedf30bde175f774638"
)
