#!/usr/bin/env -S bash ../.port_include.sh
port=sl
version=git
workdir=sl-master
files="https://github.com/mtoyoda/sl/archive/master.tar.gz sl-git.tar.gz"
depends="ncurses"

build() {
    run ${CC} -I${SERENITY_BUILD_DIR}/Root/usr/local/include/ncurses -L${SERENITY_BUILD_DIR}/Root/usr/local/lib -o sl sl.c -lncurses -ltinfo
}
