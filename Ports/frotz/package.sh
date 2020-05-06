#!/bin/bash ../.port_include.sh
port=frotz
version=git
workdir=frotz-master
files="https://gitlab.com/DavidGriffith/frotz/-/archive/master/frotz-master.zip frotz-master.zip"
depends="ncurses"

build() {
    run make \
        AR=i686-pc-serenity-ar \
        RANLIB=i686-pc-serenity-ranlib \
        PKG_CONFIG_CURSES=no \
        CURSES_CFLAGS="-I${SERENITY_ROOT}/Build/Root/usr/local/include/ncurses" \
        CURSES_LDFLAGS="-lncurses -ltinfo" \
        CURSES=ncurses \
        USE_UTF8=no \
        nosound
}
