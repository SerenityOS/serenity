#!/bin/sh
PORT_DIR=SDL
fetch() {
    run_fetch_git "https://github.com/SerenityOS/SDL"
}
configure() {
    CMAKEOPTS="-DPULSEAUDIO=OFF"
    run_configure_cmake
}
build() {
    run_make
}
install() {
    run_make_install
}
. ../.port_include.sh
