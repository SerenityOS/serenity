#!/bin/sh ../.port_include.sh
port=binutils
version=2.32
useconfigure=true
configopts="--target=i686-pc-serenity --with-sysroot=/ --with-build-sysroot=$SERENITY_ROOT/Root --disable-werror --disable-gdb --disable-nls"
files="https://ftp.gnu.org/gnu/binutils/binutils-2.32.tar.xz binutils-2.32.tar.xz"
