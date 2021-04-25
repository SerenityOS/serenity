#!/usr/bin/env -S bash ../.port_include.sh
port=Super-Mario
useconfigure=true
version=git
depends="SDL2 SDL2_mixer SDL2_image"
workdir=Super-Mario-Clone-Cpp-master
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_SOURCE_DIR/Toolchain/CMake/CMakeToolchain.txt"
files="https://github.com/Bennyhwanggggg/Super-Mario-Clone-Cpp/archive/refs/heads/master.zip master.zip fcacc15d3b5afccb3227f982d3e05f2cfeb198f0fffd008fdcda005cb7f87f91"
auth_type=sha256
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
