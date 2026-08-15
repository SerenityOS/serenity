#!/usr/bin/env -S bash ../.port_include.sh
port=sl
version='923e7d7ebc5c1f009755bdeb789ac25658ccce03'
files=(
    "https://github.com/mtoyoda/sl/archive/${version}.tar.gz#0b90e669db80437b106c49536b89a5364b47e6a55d0a0164a8dda5d2dbd2aab0"
)
depends=("ncurses")

build() {
    run ${CC} -I${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses -o sl sl.c -lncurses -ltinfo
}
