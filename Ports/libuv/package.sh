#!/usr/bin/env -S bash ../.port_include.sh
port=libuv
version=1.44.1
useconfigure=true
files=(
    "https://github.com/libuv/libuv/archive/refs/tags/v$version.tar.gz#e91614e6dc2dd0bfdd140ceace49438882206b7a6fb00b8750914e67a9ed6d6b"
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
