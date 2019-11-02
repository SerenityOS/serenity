#!/bin/sh ../.port_include.sh
port=make
version=4.2.1
useconfigure=true
files="https://ftp.gnu.org/gnu/make/make-4.2.1.tar.bz2 make-4.2.1.tar.bz2"
configopts="--target=i686-pc-serenity --with-sysroot=/ --without-guile"
