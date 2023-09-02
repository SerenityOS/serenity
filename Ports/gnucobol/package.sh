#!/usr/bin/env -S bash ../.port_include.sh
port=gnucobol
version=3.1.2
useconfigure="true"
use_fresh_config_sub="true"
config_sub_paths=("build_aux/config.sub")
depends=("gmp" "gcc" "bash" "ncurses")
files=(
    "https://ftpmirror.gnu.org/gnu/gnucobol/gnucobol-${version}.tar.bz2#11181da708dbe65c7d047baadafb4bd49d5cde9b603bec0c842576a84e293fd5"
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
    "--prefix=/usr/local"
    "--enable-hardening"
    "--disable-rpath"
    "--disable-nls"
    "--with-gnu-ld"
    "--with-dl"
    "--with-math=gmp"
    "--with-curses=ncurses"
    "--with-db=no"
    "--with-json=no"
)
