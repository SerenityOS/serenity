#!/usr/bin/env -S bash ../.port_include.sh
port='cmake'
# NOTE: keep this version aligned with Toolchain/BuildCMake.sh
version='3.26.4'
useconfigure='true'
files=(
    "https://github.com/Kitware/CMake/releases/download/v${version}/cmake-${version}.tar.gz#313b6880c291bd4fe31c0aa51d6e62659282a521e695f30d5cc0d25abbd5c208"
)
depends=(
    'bash'
    'make'
    'sed'
    'ncurses'
    'libuv'
    'openssl'
)

configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_USE_SYSTEM_LIBRARY_LIBUV=1"
    "-DCMAKE_USE_OPENSSL=ON"
    "-DBUILD_TESTING=OFF"
    "-GNinja"
)

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run ninja -j${MAKEJOBS}
}

install() {
    run ninja install
}
