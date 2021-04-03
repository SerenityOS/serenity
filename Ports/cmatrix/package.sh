#!/usr/bin/env -S bash ../.port_include.sh
port=cmatrix
useconfigure=true
version=git
depends="ncurses"
workdir=cmatrix-master
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMakeToolchain.txt"
files="https://github.com/abishekvashok/cmatrix/archive/refs/heads/master.zip cmatrix.zip"

configure() {
    run cmake $configopts
}

install() {
    run cp cmatrix "${SERENITY_BUILD_DIR}/Root/bin"
}
