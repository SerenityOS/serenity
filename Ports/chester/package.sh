#!/usr/bin/env -S bash ../.port_include.sh
port=chester
useconfigure=true
version=git
depends=("SDL2")
workdir=chester-public
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files=(
    "https://github.com/veikkos/chester/archive/public.tar.gz#b3ea7ad40608e1050fa434258f5c69b93e7bad10523c4c4a86fe08d1442a907b"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run cp bin/chester "${SERENITY_INSTALL_ROOT}/bin"
}
