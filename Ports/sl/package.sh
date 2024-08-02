#!/usr/bin/env -S bash ../.port_include.sh
port=sl
version=git
workdir=sl-master
files=(
    "https://github.com/mtoyoda/sl/archive/master.tar.gz#3270434e28c4f4e15b8e98de60ea98508a7486485f52356a61f36ac5430fbc80"
)
depends=("ncurses")

build() {
    run ${CC} -I${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses -o sl sl.c -lncurses -ltinfo
}
