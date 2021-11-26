#!/usr/bin/env -S bash ../.port_include.sh
port=ncurses
version=6.3
useconfigure=true
configopts=(
    "--enable-pc-files"
    "--enable-sigwinch"
    "--enable-term-driver"
    "--with-pkg-config=/usr/local/lib/pkgconfig"
    "--with-pkg-config-libdir=/usr/local/lib/pkgconfig"
    "--with-shared"
    "--without-ada"
    "--enable-widec"
)
files="https://invisible-mirror.net/archives/ncurses/ncurses-${version}.tar.gz ncurses-${version}.tar.gz 97fc51ac2b085d4cde31ef4d2c3122c21abc217e9090a43a30fc5ec21684e059"
auth_type="sha256"

pre_configure() {
    export CPPFLAGS="-P"
}

post_install() {
    # Compatibility symlinks for merged libraries.
    for lib in tinfo tic curses; do
        ln -svf libncursesw.so "${SERENITY_INSTALL_ROOT}/usr/local/lib/lib${lib}w.so"
    done

    # Compatibility symlinks for non-w libraries.
    for lib in form menu ncurses ncurses++ panel tinfo tic curses; do
        ln -svf lib${lib}w.so "${SERENITY_INSTALL_ROOT}/usr/local/lib/lib${lib}.so"
    done

    # Compatibility symlink for the include folder.
    # Target folder has to be removed, otherwise we will get `/usr/local/include/ncurses/ncursesw`.
    rm -rf "${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses"
    ln -svf ncursesw "${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses"
}
