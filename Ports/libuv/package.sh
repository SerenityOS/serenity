#!/usr/bin/env -S bash ../.port_include.sh
port=libuv
version=1.42.0
useconfigure=true
files="https://github.com/libuv/libuv/archive/refs/tags/v$version.tar.gz $port-$version.tar.gz 371e5419708f6aaeb8656671f89400b92a9bba6443369af1bb70bcd6e4b3c764"
auth_type=sha256
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
