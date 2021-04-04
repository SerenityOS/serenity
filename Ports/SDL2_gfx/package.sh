#!/usr/bin/env -S bash ../.port_include.sh

port=SDL2_gfx
version=1.0.4
files="https://downloads.sourceforge.net/project/sdl2gfx/SDL2_gfx-${version}.tar.gz SDL2_gfx-${version}.tar.gz"
depends="SDL2"
useconfigure=true
configopts="--with-sdl-prefix=${SERENITY_BUILD_DIR}/Root/usr"
