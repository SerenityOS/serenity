#!/usr/bin/env -S bash ../.port_include.sh
port='aclock'
version='2.3'
files=(
    "https://github.com/tenox7/aclock/raw/f9668617eac365fe8b5416cfbd801b727d8a3746/sources/aclock-unix-curses.c#2b098996409c4740f492fb8fd5a63cb5e3e15283c2f7e3f06c75d6a9ad916669"
)
depends=("ncurses")

build() {
    run_nocd ${CC} aclock-unix-curses.c -o aclock -lcurses -lm
}

install() {
    cp aclock "$DESTDIR/usr/local/bin"
}
