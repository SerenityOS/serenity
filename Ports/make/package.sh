#!/bin/bash ../.port_include.sh
port=make
version=4.2.1
useconfigure=true
files="https://ftp.gnu.org/gnu/make/make-${version}.tar.bz2 make-${version}.tar.bz2
http://ftp.gnu.org/gnu/make/make-${version}.tar.bz2.sig make-${version}.tar.bz2.sig
https://ftp.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg make-${version}.tar.bz2.sig"
configopts="--target=i686-pc-serenity --with-sysroot=/ --without-guile"
