#!/usr/bin/env -S bash ../.port_include.sh
port='cmake'
version='3.26.0'
useconfigure='true'
files="https://github.com/Kitware/CMake/releases/download/v${version}/cmake-${version}.tar.gz cmake-${version}.tar.gz 4256613188857e95700621f7cdaaeb954f3546a9249e942bc2f9b3c26e381365"
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
