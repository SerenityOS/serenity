#!/usr/bin/env -S bash ../.port_include.sh
port='frotz'
version='2.54'
files=(
    "https://gitlab.com/DavidGriffith/frotz/-/archive/${version}/frotz-${version}.tar.bz2 frotz-${version}.tar.bz2 bdf9131e6de49108c9f032200cea3cb4011e5ca0c9fbdbf5b0c05f7c56c81395"
)
depends=("ncurses")

build() {
    run make \
        PKG_CONFIG_CURSES=no \
        CURSES_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses" \
        CURSES_LDFLAGS="-lncurses -ltinfo" \
        CURSES=ncurses \
        USE_UTF8=no \
        nosound
}
