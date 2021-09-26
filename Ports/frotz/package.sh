#!/usr/bin/env -S bash ../.port_include.sh
port=frotz
version=2.53
files="https://gitlab.com/DavidGriffith/frotz/-/archive/${version}/frotz-${version}.tar.bz2 frotz-${version}.tar.bz2 8da558828dd74d6d6ee30483bb32276ef918b8b72b7f6e89b4f7cb27e7abf58b"
auth_type=sha256
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
