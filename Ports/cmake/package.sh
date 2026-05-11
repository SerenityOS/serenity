#!/usr/bin/env -S bash ../.port_include.sh
port='cmake'
# NOTE: keep this version aligned with Toolchain/BuildCMake.sh
version='3.30.3'
useconfigure='true'
files=(
    "https://github.com/Kitware/CMake/releases/download/v${version}/cmake-${version}.tar.gz#6d5de15b6715091df7f5441007425264bdd477809f80333fdf95f846aaff88e4"
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
