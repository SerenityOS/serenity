#!/usr/bin/env -S bash ../.port_include.sh
port=libuv
version=1.52.1
useconfigure=true
files=(
    "https://github.com/libuv/libuv/archive/refs/tags/v$version.tar.gz#478baf2599bfbc882c355288c9cb6f92e0e7dda435fa04031fa5b607cf3f414c"
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
