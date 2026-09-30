#!/usr/bin/env -S bash ../.port_include.sh
port=libuv
version=1.53.0
useconfigure=true
files=(
    "https://github.com/libuv/libuv/archive/refs/tags/v$version.tar.gz#279f3f67a24bb9921fe999ca6cd5e332fade8d515873ef9ba054b70e70a31d9e"
)
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-GNinja" "-DCMAKE_BUILD_WITH_INSTALL_RPATH=true" "-DBUILD_TESTING=OFF")

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run ninja
}

install() {
    run ninja install
}
