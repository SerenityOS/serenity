#!/usr/bin/env -S bash ../.port_include.sh
port=angband
version=4.2.3
workdir="Angband-${version}"
useconfigure=true
files="https://github.com/angband/angband/releases/download/${version}/Angband-${version}.tar.gz Angband-${version}.tar.gz 833c4f8cff2aee61ad015f9346fceaa4a8c739fe2dbe5bd1acd580c91818e6bb"
auth_type=sha256
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
