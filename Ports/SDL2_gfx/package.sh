#!/usr/bin/env -S bash ../.port_include.sh

port=SDL2_gfx
version=1.0.4
files=(
    "https://downloads.sourceforge.net/project/sdl2gfx/SDL2_gfx-${version}.tar.gz#63e0e01addedc9df2f85b93a248f06e8a04affa014a835c2ea34bfe34e576262"
)
depends=("SDL2")
useconfigure=true
use_fresh_config_sub=true
configopts=("--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local" "--disable-static" "--enable-shared")
