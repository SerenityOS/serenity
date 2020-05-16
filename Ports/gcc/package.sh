#!/bin/bash ../.port_include.sh
port=gcc
version=10.1.0
useconfigure=true
configopts="--target=i686-pc-serenity --with-sysroot=/ --with-build-sysroot=$SERENITY_ROOT/Build/Root --with-newlib --enable-languages=c,c++ --disable-lto --disable-nls"
files="https://ftp.gnu.org/gnu/gcc/gcc-${version}/gcc-${version}.tar.xz gcc-${version}.tar.xz
https://ftp.gnu.org/gnu/gcc/gcc-${version}/gcc-${version}.tar.xz.sig gcc-${version}.tar.xz.sig
https://ftp.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
makeopts="all-gcc all-target-libgcc all-target-libstdc++-v3 -j $(nproc)"
installopts="DESTDIR=$SERENITY_ROOT/Build/Root install-gcc install-target-libgcc install-target-libstdc++-v3"
depends="binutils"
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg gcc-${version}.tar.xz.sig"
post_fetch() {
    run contrib/download_prerequisites
}
build() {
    run make $makeopts
    run find ./host-i686-pc-serenity/gcc/ -maxdepth 1 -type f -executable -exec strip --strip-debug {} \; || echo
}

install() {
    run make $installopts
}
