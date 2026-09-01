#!/usr/bin/env -S bash ../.port_include.sh
port=sl
version='5.05'
files=(
    "https://github.com/eyJhb/sl/archive/${version}.tar.gz#6c941b526e3d01be7f91a3af4ae20a89d1e5d66b3b2d804c80123b1b1be96384"
)
depends=("ncurses")

build() {
    run ${CC} -I${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses -o sl sl.c -lncurses -ltinfo
}
