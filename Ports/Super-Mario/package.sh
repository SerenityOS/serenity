#!/usr/bin/env -S bash ../.port_include.sh
port=Super-Mario
useconfigure=true
version=git
depends="SDL2 SDL2_mixer SDL2_image"
workdir=Super-Mario-Clone-Cpp-master
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_SOURCE_DIR/Toolchain/CMake/CMakeToolchain.txt"
files="https://github.com/Bennyhwanggggg/Super-Mario-Clone-Cpp/archive/refs/heads/master.zip master.zip 11f622721d1ba504acf75c024aa0dbe3"
auth_type=md5
launcher_name="Super Mario"
launcher_category=Games
launcher_command=/opt/Super_Mario/uMario

configure() {
    run cmake $configopts
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/opt/Super_Mario"
    run cp -r uMario app.ico icon2.ico files "${SERENITY_INSTALL_ROOT}/opt/Super_Mario" 
    install_launcher
}
