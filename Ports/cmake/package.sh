#!/usr/bin/env -S bash ../.port_include.sh
port='cmake'
version='3.25.1'
useconfigure='true'
files="https://github.com/Kitware/CMake/releases/download/v${version}/cmake-${version}.tar.gz cmake-${version}.tar.gz 1c511d09516af493694ed9baf13c55947a36389674d657a2d5e0ccedc6b291d8"
auth_type='sha256'
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
