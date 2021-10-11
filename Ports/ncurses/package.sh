#!/usr/bin/env -S bash ../.port_include.sh
port=ncurses
version=6.2
useconfigure=true
configopts=(
    "--enable-pc-files"
    "--enable-sigwinch"
    "--enable-term-driver"
    "--with-pkg-config=/usr/local/lib/pkgconfig"
    "--with-pkg-config-libdir=/usr/local/lib/pkgconfig"
    "--with-shared"
    "--without-ada"
)
files="https://ftpmirror.gnu.org/gnu/ncurses/ncurses-${version}.tar.gz ncurses-${version}.tar.gz 30306e0c76e0f9f1f0de987cf1c82a5c21e1ce6568b9227f7da5b71cbea86c9d"
auth_type="sha256"

pre_configure() {
    export CPPFLAGS="-P"
}

post_install() {
    ln -sv libncurses.so "${SERENITY_INSTALL_ROOT}/usr/local/lib/libcurses.so"
    ln -sv libncurses.so "${SERENITY_INSTALL_ROOT}/usr/local/lib/libtic.so"
    ln -sv libncurses.so "${SERENITY_INSTALL_ROOT}/usr/local/lib/libtinfo.so"
}
