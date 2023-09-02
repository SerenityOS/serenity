#!/usr/bin/env -S bash ../.port_include.sh
port='frotz'
version='2.54'
files=(
    "https://gitlab.com/DavidGriffith/frotz/-/archive/${version}/frotz-${version}.tar.bz2#756d7e11370c9c8e61573e350e2a5071e77fd2781be74c107bd432f817f3abc7"
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
