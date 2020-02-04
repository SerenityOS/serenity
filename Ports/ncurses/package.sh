#!/bin/bash ../.port_include.sh
port=ncurses
version=6.1
useconfigure=true
configopts="--with-termlib --enable-pc-files --with-pkg-config=/usr/local/lib/pkgconfig --with-pkg-config-libdir=/usr/local/lib/pkgconfig"
files="ftp://ftp.gnu.org/gnu/ncurses/ncurses-${version}.tar.gz ncurses-${version}.tar.gz
http://ftp.gnu.org/gnu/ncurses/ncurses-${version}.tar.gz.sig ncurses-${version}.tar.gz.sig
https://ftp.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg ncurses-${version}.tar.gz.sig"