#!/usr/bin/env -S bash ../.port_include.sh
port='frotz'
version='2.55'
files=(
    "https://gitlab.com/DavidGriffith/frotz/-/archive/${version}/frotz-${version}.tar.bz2#92051a90c55fdcdf8c4336add2bf09b8329b468f3c0d2739dd850c00d1b9c8f1"
)
depends=("ncurses")
makeopts=(
    "PKG_CONFIG_CURSES=no"
    "CURSES_CFLAGS=-I${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses"
    "CURSES_LDFLAGS=-lncurses -ltinfo"
    "CURSES=ncurses"
    "USE_UTF8=no"
    "nosound"
)
installopts=(
    "nosound"
)
