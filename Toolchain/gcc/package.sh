#!/bin/bash ../.toolchain_include.sh

package=gcc
version=9.2.0
useconfigure=true

configopts="--target=$TARGET --with-sysroot=$SYSROOT --disable-nls --with-newlib --enable-shared --enable-languages=c,c++"
makeopts="all-gcc all-target-libgcc"
installopts="install-gcc install-target-libgcc"
depends="binutils"

filename="gcc-${version}.tar.gz"
files="https://ftp.gnu.org/gnu/gcc/gcc-${version}/${filename} ${filename} e03739b042a14376d727ddcfd05a9bc3"

postfetch() {
    if [ `uname -s` = "OpenBSD" ]; then
        run_sourcedir perl -pi -e 's/-no-pie/-nopie/g' gcc/configure
    fi
}
postinstall() {
    if [ `uname -s` = "OpenBSD" ]; then
        cd "$PREFIX/libexec/gcc/${TARGET}/${version}" && ln -sf liblto_plugin.so.0.0 liblto_plugin.so
    fi
}
