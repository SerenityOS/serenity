#!/usr/bin/env -S bash ../.port_include.sh
port=cmake
version=3.23.1
useconfigure=true
files="https://github.com/Kitware/CMake/releases/download/v$version/cmake-$version.tar.gz cmake-$version.tar.gz 33fd10a8ec687a4d0d5b42473f10459bb92b3ae7def2b745dc10b192760869f3"
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
