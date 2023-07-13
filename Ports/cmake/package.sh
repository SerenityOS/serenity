#!/usr/bin/env -S bash ../.port_include.sh
port='cmake'
version='3.26.1'
useconfigure='true'
files="https://github.com/Kitware/CMake/releases/download/v${version}/cmake-${version}.tar.gz cmake-${version}.tar.gz f29964290ad3ced782a1e58ca9fda394a82406a647e24d6afd4e6c32e42c412f"
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
