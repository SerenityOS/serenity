#!/usr/bin/env -S bash ../.port_include.sh
port=cmake
version=3.23.0
useconfigure=true
files="https://github.com/Kitware/CMake/releases/download/v$version/cmake-$version.tar.gz cmake-$version.tar.gz 5ab0a12f702f44013be7e19534cd9094d65cc9fe7b2cd0f8c9e5318e0fe4ac82"
auth_type=sha256
depends=("bash" "make" "sed" "ncurses" "libuv" "openssl")
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DCMAKE_USE_SYSTEM_LIBRARY_LIBUV=1" "-DCMAKE_USE_OPENSSL=ON" "-GNinja")

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run ninja -j${MAKEJOBS}
}

install() {
    run ninja install
}
