#!/usr/bin/env -S bash ../.port_include.sh
port=bochs
version=2.7
depends=("SDL2")
files=(
    "https://download.sourceforge.net/project/bochs/bochs/$version/bochs-$version.tar.gz#a010ab1bfdc72ac5a08d2e2412cd471c0febd66af1d9349bc0d796879de5b17a"
)
useconfigure=true
use_fresh_config_sub=true
configopts=("--with-sdl2")
