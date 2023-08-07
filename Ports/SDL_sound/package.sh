#!/usr/bin/env -S bash ../.port_include.sh
port='SDL_sound'
version='1.0.3'
useconfigure='true'
use_fresh_config_sub='true'
depends=("sdl12-compat" "libmikmod")
files=(
    "https://www.icculus.org/SDL_sound/downloads/${port}-${version}.tar.gz 3999fd0bbb485289a52be14b2f68b571cb84e380cc43387eadf778f64c79e6df"
)
configopts=(
    "--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "--enable-ogg=no"
    "--enable-modplug=no"
)
makeopts=("LDFLAGS=-lm")
