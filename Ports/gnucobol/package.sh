#!/usr/bin/env -S bash ../.port_include.sh
port=gnucobol
version=3.1.2
useconfigure="true"
depends="gmp gcc bash"
files="https://ftp.gnu.org/gnu/gnucobol/gnucobol-${version}.tar.bz2 gnucobol-${version}.tar.bz2
https://ftp.gnu.org/gnu/gnucobol/gnucobol-${version}.tar.bz2.sig gnucobol-${version}.tar.bz2.sig
https://ftp.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg gnucobol-${version}.tar.bz2.sig"
configopts="--prefix=/usr/local --enable-hardening --disable-rpath --with-gnu-ld --with-dl --with-math=gmp --with-db=no --with-json=no --with-curses=curses"
