#!/usr/bin/env -S bash ../.port_include.sh
port=sl
version=git
workdir=sl-master
files="https://github.com/mtoyoda/sl/archive/master.tar.gz sl-git.tar.gz 230347a534644a46e635877a6b0dfb77"
auth_type=md5
depends="ncurses"

build() {
    run ${CC} -I${SERENITY_BUILD_DIR}/Root/usr/local/include/ncurses -L${SERENITY_BUILD_DIR}/Root/usr/local/lib -o sl sl.c -lncurses -ltinfo
}
