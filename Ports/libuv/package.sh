#!/usr/bin/env -S bash ../.port_include.sh
port=libuv
version=1.51.0
useconfigure=true
files=(
    "https://github.com/libuv/libuv/archive/refs/tags/v$version.tar.gz#27e55cf7083913bfb6826ca78cde9de7647cded648d35f24163f2d31bb9f51cd"
)
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-GNinja" "-DCMAKE_BUILD_WITH_INSTALL_RPATH=true")

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run ninja
}

install() {
    run ninja install
}
