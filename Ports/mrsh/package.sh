#!/usr/bin/env -S bash ../.port_include.sh
port=mrsh
version=cd3c3a48055ab4085d83f149ff4b4feba40b40cb
files=(
    "https://github.com/emersion/mrsh/archive/${version}.tar.gz#d26e3fdee71ef168cf3f8ad2912c148b20aab524048e4ea899d6b83fb299ceab"
)
useconfigure=true
configopts=(
    "--without-readline"
)

export CFLAGS=-Wno-deprecated-declarations
