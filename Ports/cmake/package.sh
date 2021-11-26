#!/usr/bin/env -S bash ../.port_include.sh
port=cmake
version=3.19.4
useconfigure=true
files="https://github.com/Kitware/CMake/releases/download/v$version/cmake-$version.tar.gz cmake-$version.tar.gz 7d0232b9f1c57e8de81f38071ef8203e6820fe7eec8ae46a1df125d88dbcc2e1"
auth_type=sha256
depends=("bash" "make" "sed" "ncurses" "libuv")
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DCMAKE_USE_SYSTEM_LIBRARY_LIBUV=1" "-GNinja")

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run ninja
}

install() {
    run ninja install
}
