#!/usr/bin/env -S bash ../.port_include.sh
port=cmake
version=3.22.1
useconfigure=true
files="https://github.com/Kitware/CMake/releases/download/v$version/cmake-$version.tar.gz cmake-$version.tar.gz 0e998229549d7b3f368703d20e248e7ee1f853910d42704aa87918c213ea82c0"
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
