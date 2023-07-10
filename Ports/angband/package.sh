#!/usr/bin/env -S bash ../.port_include.sh
port=angband
version=4.2.4
workdir="Angband-${version}"
useconfigure=true
use_fresh_config_sub=true
files=(
    "https://github.com/angband/angband/releases/download/${version}/Angband-${version}.tar.gz Angband-${version}.tar.gz a07c78c1dd05e48ddbe4d8ef5d1880fcdeab55fd05f1336d9cba5dd110b15ff3"
)
depends=("ncurses" "SDL2" "SDL2_image" "SDL2_ttf" "SDL2_mixer")
configopts=(
    "--prefix=/usr/local"
    "--bindir=/usr/local/bin"
    "--disable-x11"
    "--enable-curses"
    "--enable-sdl2"
    "--enable-sdl2-mixer"
    "--with-ncurses-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "--with-sdl2-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "CFLAGS=--sysroot=${SERENITY_INSTALL_ROOT} -I${SERENITY_INSTALL_ROOT}/usr/local/include/ncursesw"
    "LIBS=-lncursesw"
)
