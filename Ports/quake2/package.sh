#!/usr/bin/env -S bash ../.port_include.sh
port=quake2
version=0.1
workdir=SerenityQuakeII-master
useconfigure=true
files="https://github.com/SerenityPorts/SerenityQuakeII/archive/master.tar.gz quake2.tar.gz"
makeopts=()
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
depends=("SDL2")
launcher_name=QuakeII
launcher_category=Games
launcher_command=quake2

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
