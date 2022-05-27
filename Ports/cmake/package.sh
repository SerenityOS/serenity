#!/usr/bin/env -S bash ../.port_include.sh
port=cmake
version=3.23.2
useconfigure=true
files="https://github.com/Kitware/CMake/releases/download/v$version/cmake-$version.tar.gz cmake-$version.tar.gz f316b40053466f9a416adf981efda41b160ca859e97f6a484b447ea299ff26aa"
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
