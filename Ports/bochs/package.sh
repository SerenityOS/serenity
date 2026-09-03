#!/usr/bin/env -S bash ../.port_include.sh
port='bochs'
version='3.1'
depends=("SDL2")
files=(
    "https://download.sourceforge.net/project/bochs/bochs/$version/bochs-$version.tar.gz#14aaf78dbe1337987923fffc4e7a962ae56abcf9a87474ace39e593f9f84ee84"
)
workdir='bochs'
use_fresh_config_sub='true'
useconfigure='true'
configopts=("--with-sdl2")
