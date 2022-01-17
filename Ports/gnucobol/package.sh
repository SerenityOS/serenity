#!/usr/bin/env -S bash ../.port_include.sh
port=gnucobol
version=3.1.2
useconfigure="true"
use_fresh_config_sub="true"
depends=("gmp" "gcc" "bash" "ncurses")
files="https://ftpmirror.gnu.org/gnu/gnucobol/gnucobol-${version}.tar.bz2 gnucobol-${version}.tar.bz2
https://ftpmirror.gnu.org/gnu/gnucobol/gnucobol-${version}.tar.bz2.sig gnucobol-${version}.tar.bz2.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "gnucobol-${version}.tar.bz2.sig")
configopts=(
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
