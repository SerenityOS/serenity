#!/usr/bin/env -S bash ../.port_include.sh
port=PrinceOfPersia
useconfigure=true
version=git
depends="SDL2 SDL2_image"
workdir=SDLPoP-master
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMake/CMakeToolchain.txt"
files="https://github.com/NagyD/SDLPoP/archive/refs/heads/master.zip PoP.zip c75184eb2a7e8c9ed008ffae371ec178"
auth_type=md5
install_location="Root/opt/PrinceOfPersia"

configure() {
    run cmake $configopts src/.
}

install() {
    run mkdir -p "${SERENITY_BUILD_DIR}/${install_location}"
    run cp -r prince data SDLPoP.ini "${SERENITY_BUILD_DIR}/${install_location}" 
}
