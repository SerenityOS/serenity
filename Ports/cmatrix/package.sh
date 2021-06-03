#!/usr/bin/env -S bash ../.port_include.sh
port=cmatrix
useconfigure=true
version=git
depends="ncurses"
workdir=cmatrix-master
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_SOURCE_DIR/Toolchain/CMake/CMakeToolchain.txt"
files="https://github.com/abishekvashok/cmatrix/archive/refs/heads/master.zip cmatrix.zip c32ca7562e58fb1fd7a96ebdfbe51c5de060709d39b67fce3c0bc42547e0ccb2"
auth_type=sha256
launcher_name=cmatrix
launcher_category=Games
launcher_command="Terminal -e cmatrix"

configure() {
    run cmake $configopts
}

install() {
    run cp cmatrix "${SERENITY_INSTALL_ROOT}/bin"
}
