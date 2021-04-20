#!/usr/bin/env -S bash ../.port_include.sh
port=c-ray
version=c094d64570c30c70f4003e9428d31a2a0d9d3d41
useconfigure=true
files="https://github.com/vkoskiv/c-ray/archive/${version}.tar.gz ${version}.tar.gz b83e3c6a1462486257dfe38d309b47c2"
auth_type=md5
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_SOURCE_DIR/Toolchain/CMake/CMakeToolchain.txt"
depends="SDL2"
workdir="${port}-${version}"

configure() {
    run cmake $configopts
}

install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/home/anon/c-ray"
    cp -r "${port}-${version}"/* "${SERENITY_INSTALL_ROOT}/home/anon/c-ray"
}
