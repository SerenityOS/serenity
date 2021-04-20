#!/usr/bin/env -S bash ../.port_include.sh
port=cmatrix
useconfigure=true
version=git
depends="ncurses"
workdir=cmatrix-master
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_SOURCE_DIR/Toolchain/CMake/CMakeToolchain.txt"
files="https://github.com/abishekvashok/cmatrix/archive/refs/heads/master.zip cmatrix.zip 2541321b89149b375d5732402e52d654"
auth_type=md5
launcher_name=cmatrix
launcher_category=Games
launcher_command="Terminal -e cmatrix"

configure() {
    run cmake $configopts
}

install() {
    run cp cmatrix "${SERENITY_INSTALL_ROOT}/bin"
    install_launcher
}
