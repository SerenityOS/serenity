#!/bin/bash ../.port_include.sh
port=sl
version=git
workdir=sl-master
files="https://github.com/mtoyoda/sl/archive/master.tar.gz sl-git.tar.gz"
depends="ncurses"

build() {
    run ${CC} -I${SERENITY_ROOT}/Build/Root/usr/local/include/ncurses -L${SERENITY_ROOT}/Build/Root/usr/local/lib -o sl sl.c -lncurses -ltinfo
}

post_install() {
    # Dirty hack that seems to be necessary to make ncurses play nice
    mkdir -p ${SERENITY_ROOT}/Build/Root/usr/local/share/terminfo/x
    cp ${SERENITY_ROOT}/Build/Root/usr/local/share/terminfo/78/xterm ${SERENITY_ROOT}/Build/Root/usr/local/share/terminfo/x/
}
