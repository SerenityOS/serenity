#!/usr/bin/env -S bash ../.port_include.sh
port=gcc
version=10.3.0
useconfigure=true
configopts="--target=${SERENITY_ARCH}-pc-serenity --with-sysroot=/ --with-build-sysroot=${SERENITY_INSTALL_ROOT} --with-newlib --enable-languages=c,c++ --disable-lto --disable-nls --enable-shared --enable-default-pie --enable-host-shared"
files="https://ftpmirror.gnu.org/gnu/gcc/gcc-${version}/gcc-${version}.tar.xz gcc-${version}.tar.xz
https://ftpmirror.gnu.org/gnu/gcc/gcc-${version}/gcc-${version}.tar.xz.sig gcc-${version}.tar.xz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
makeopts="all-gcc all-target-libgcc all-target-libstdc++-v3 -j $(nproc)"
installopts="DESTDIR=${SERENITY_INSTALL_ROOT} install-gcc install-target-libgcc install-target-libstdc++-v3"
depends="binutils"
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg gcc-${version}.tar.xz.sig"

post_fetch() {
    run contrib/download_prerequisites
}

pre_configure() {
    patch_internal
    run sed -i 's@-fno-exceptions @@' gcc/config/serenity.h
}

build() {
    run make $makeopts
    run find "./host-${SERENITY_ARCH}-pc-serenity/gcc/" -maxdepth 1 -type f -executable -exec strip --strip-debug {} \; || echo
}

install() {
    run make $installopts
}
