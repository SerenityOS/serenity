#!/usr/bin/env -S bash ../.port_include.sh
port='wabt'
version='1.0.42'
useconfigure='true'
files=(
    "https://github.com/WebAssembly/wabt/releases/download/${version}/wabt-${version}.tar.xz#a76cda3c174a43097863a07fc0b0c202f770f53e21806ea2636f167d1ffb1e30"
)
configopts=(
    '-Bbuild'
    '-DWITH_WASI=ON'
    '-DCMAKE_BUILD_TYPE=Release'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
depends=(
    'uvwasi'
)
configure() {
    run cmake "${configopts[@]}"
}

build() {
    run cmake --build build
}

install() {
    run cmake --install build
}
