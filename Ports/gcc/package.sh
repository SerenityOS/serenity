#!/usr/bin/env -S bash ../.port_include.sh
port=gcc
version=11.1.0
useconfigure=true
configopts="--target=${SERENITY_ARCH}-pc-serenity --with-sysroot=/ --with-build-sysroot=${SERENITY_INSTALL_ROOT} --with-newlib --enable-languages=c,c++ --disable-lto --disable-nls --enable-shared --enable-default-pie --enable-host-shared --enable-threads=posix"
files="https://ftpmirror.gnu.org/gnu/gcc/gcc-${version}/gcc-${version}.tar.xz gcc-${version}.tar.xz 4c4a6fb8a8396059241c2e674b85b351c26a5d678274007f076957afa1cc9ddf"
makeopts="all-gcc all-target-libgcc all-target-libstdc++-v3 -j $(nproc)"
installopts="DESTDIR=${SERENITY_INSTALL_ROOT} install-gcc install-target-libgcc install-target-libstdc++-v3"
depends="binutils"
auth_type="sha256"

post_fetch() {
    run contrib/download_prerequisites
}

pre_configure() {
    patch_internal
    run sed -i.bak 's@-fno-exceptions @@' gcc/config/serenity.h
    run rm -f gcc/config/serenity.h.bak
}

build() {
    run make $makeopts
    run find "./host-${SERENITY_ARCH}-pc-serenity/gcc/" -maxdepth 1 -type f -executable -exec strip --strip-debug {} \; || echo
}

install() {
    run make $installopts
}
