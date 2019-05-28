#!/bin/sh
PORT_DIR=binutils
function fetch() {
    # Canonical repository:
    # run_fetch_git "http://sourceware.org/git/binutils-gdb.git"

    # Much faster mirror (though unofficial):
    run_fetch_git "https://github.com/bminor/binutils-gdb.git"

    # FIXME: It would probably be better to build from a tarball.
    run_command git reset --hard binutils-2_32

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
