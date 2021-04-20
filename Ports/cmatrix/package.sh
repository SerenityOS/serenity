#!/usr/bin/env -S bash ../.port_include.sh
port=cmatrix
useconfigure=true
version=git
depends="ncurses"
workdir=cmatrix-master
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_SOURCE_DIR/Toolchain/CMake/CMakeToolchain.txt"
files="https://github.com/abishekvashok/cmatrix/archive/refs/heads/master.zip cmatrix.zip 2541321b89149b375d5732402e52d654"
auth_type=md5

configure() {
    run cmake $configopts
}

install() {
    run cp cmatrix "${SERENITY_BUILD_DIR}/Root/bin"
}
