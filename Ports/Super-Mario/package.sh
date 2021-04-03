#!/usr/bin/env -S bash ../.port_include.sh
port=Super-Mario
useconfigure=true
version=git
depends="SDL2 SDL2_mixer SDL2_image"
workdir=Super-Mario-Clone-Cpp-master
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMakeToolchain.txt"
files="https://github.com/Bennyhwanggggg/Super-Mario-Clone-Cpp/archive/refs/heads/master.zip master.zip"
install_location="Root/opt/Super_Mario"

configure() {
    run cmake $configopts
}

install() {
    run mkdir -p "${SERENITY_BUILD_DIR}/${install_location}"
    run cp -r uMario app.ico icon2.ico files "${SERENITY_BUILD_DIR}/${install_location}" 
}
