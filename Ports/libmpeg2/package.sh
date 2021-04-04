#!/usr/bin/env -S bash ../.port_include.sh
port=libmpeg2
version=0.5.1
useconfigure=true
files="https://libmpeg2.sourceforge.io/files/libmpeg2-${version}.tar.gz libmpeg2-${version}.tar.gz"
configopts="--disable-sdl"
