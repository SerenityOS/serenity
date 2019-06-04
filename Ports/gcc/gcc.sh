#!/bin/sh
PORT_DIR=gcc
fetch() {
    run_fetch_web "https://ftp.gnu.org/gnu/gcc/gcc-8.3.0/gcc-8.3.0.tar.xz"

    # Add the big GCC patch (same one used by toolchain.)
    run_patch $SERENITY_ROOT/Toolchain/Patches/gcc.patch -p1

    # Let GCC download mpfr, mpc and isl.
    run_command contrib/download_prerequisites

    # Patch mpfr, mpc and isl to teach them about "serenity" targets.
    run_patch dependencies-config.patch -p1
}
configure() {
    run_configure_autotools \
        --target=i686-pc-serenity \
        --with-sysroot=/ \
        --with-build-sysroot=$SERENITY_ROOT/Root \
        --with-newlib \
        --enable-languages=c,c++ \
        --disable-lto \
        --disable-nls
}
build() {
    MAKEOPTS=""
    run_make all-gcc all-target-libgcc all-target-libstdc++-v3
    run_command find ./host-i686-pc-serenity/gcc/ -maxdepth 1 -type f -executable -exec strip --strip-debug {} \; || echo
}
install() {
    run_make $INSTALLOPTS DESTDIR="$SERENITY_ROOT"/Root install-gcc install-target-libgcc install-target-libstdc++-v3
}
. ../.port_include.sh
