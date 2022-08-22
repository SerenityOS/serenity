#!/usr/bin/env -S bash ../.port_include.sh
port=aclock
version=2.3
# the github repo contains way too many different ports for other platforms and binaries, only fetch the file we need
files="https://github.com/tenox7/aclock/raw/master/sources/aclock-unix-curses.c aclock-${version}.c 2b098996409c4740f492fb8fd5a63cb5e3e15283c2f7e3f06c75d6a9ad916669"
auth_type=sha256
depends=(ncurses)

build() {
    run_nocd ${CC} aclock-${version}.c -o aclock -lcurses -lm
}

install() {
    cp aclock $DESTDIR/usr/local/bin
}
