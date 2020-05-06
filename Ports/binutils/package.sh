#!/bin/bash ../.port_include.sh
port=binutils
version=2.32
useconfigure=true
configopts="--target=i686-pc-serenity --with-sysroot=/ --with-build-sysroot=$SERENITY_ROOT/Build/Root --disable-werror --disable-gdb --disable-nls"
files="https://ftp.gnu.org/gnu/binutils/binutils-${version}.tar.xz binutils-${version}.tar.xz
https://ftp.gnu.org/gnu/binutils/binutils-${version}.tar.xz.sig binutils-${version}.tar.xz.sig
https://ftp.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg binutils-${version}.tar.xz.sig"
export ac_cv_func_getrusage=no
