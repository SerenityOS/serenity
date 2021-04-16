#!/usr/bin/env -S bash ../.port_include.sh
port=chester
useconfigure=true
version=git
depends="SDL2"
workdir=chester-public
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMake/CMakeToolchain.txt"
files="https://github.com/veikkos/chester/archive/public.tar.gz chester.tar.gz f09d797209e7bfd9b1460d2540525186"
auth_type=md5

configure() {
    run cmake $configopts
}

install() {
    run cp bin/chester "${SERENITY_BUILD_DIR}/Root/bin"
}
