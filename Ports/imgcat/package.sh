#!/usr/bin/env -S bash ../.port_include.sh
port=imgcat
version=2.5.0
depends="ncurses"
files="https://github.com/eddieantonio/imgcat/releases/download/v${version}/imgcat-${version}.tar.gz imgcat-v${version}.tar.gz 16e5386f5ce2237643785e7c8bb2cfce"
auth_type=md5

build() {
    run make \
        production=true
}

export CPPFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses"
