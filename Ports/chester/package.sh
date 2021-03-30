#!/usr/bin/env -S bash ../.port_include.sh
port=chester
useconfigure=true
version=git
depends="SDL2"
workdir=chester-public
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMakeToolchain.txt"
files="https://github.com/veikkos/chester/archive/public.tar.gz chester.tar.gz"

configure() {
    run cmake $configopts
}

install() {
    run cp bin/chester "${SERENITY_BUILD_DIR}/Root/bin"
}
