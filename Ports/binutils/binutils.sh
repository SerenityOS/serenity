#!/bin/bash
PORT_DIR=binutils
function fetch() {
    run_fetch_web "https://ftp.gnu.org/gnu/binutils/binutils-2.32.tar.xz"

    # Add the big binutils patch (same one used by toolchain.)
    run_patch $SERENITY_ROOT/Toolchain/Patches/binutils.patch -p1
}
function configure() {
    run_configure_autotools \
        --target=i686-pc-serenity \
        --with-sysroot=/ \
        --with-build-sysroot=$SERENITY_ROOT/Root \
        --disable-werror \
        --disable-gdb \
        --disable-nls
}
function build() {
    run_make
}
function install() {
    run_make_install DESTDIR="$SERENITY_ROOT"/Root
}
source ../.port_include.sh
