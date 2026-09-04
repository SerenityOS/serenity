#!/usr/bin/env -S bash ../.port_include.sh
port='liboggz'
version='1.1.3'
useconfigure='true'
files=(
    "https://downloads.xiph.org/releases/liboggz/liboggz-${version}.tar.gz#2466d03b67ef0bcba0e10fb352d1a9ffd9f96911657abce3cbb6ba429c656e2f"
)
depends=("libogg")
