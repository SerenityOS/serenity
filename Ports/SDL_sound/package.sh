#!/usr/bin/env -S bash ../.port_include.sh
port='SDL_sound'
version='1.0.3'
useconfigure='true'
use_fresh_config_sub='true'
depends=(
    'libmikmod'
    'sdl12-compat'
)
files=(
    "https://www.icculus.org/SDL_sound/downloads/${port}-${version}.tar.gz 3999fd0bbb485289a52be14b2f68b571cb84e380cc43387eadf778f64c79e6df"
)
configopts=(
    '--enable-modplug=no'
    '--enable-ogg=no'
    "--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local"    
)
makeopts=(
    'LDFLAGS=-lm'
)
