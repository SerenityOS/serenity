#!/usr/bin/env -S bash ../.port_include.sh
port='unibilium'
version='2.1.4'
files=(
    "https://github.com/neovim/unibilium/archive/refs/tags/v${version}.tar.gz#7360907bcf79ba49f6fc4a504767ff86e93ab9018477026fcc70d5ab77e1f2c1"
)
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DBUILD_MAN_PAGES=OFF'
    '-DBUILD_TESTS=OFF'
    '-DBUILD_TOOLS=OFF'
)

configure() {
    run cmake -G Ninja -B build -S . "${configopts[@]}"
}

build() {
    run cmake --build build
}

install() {
    run cmake --install build
}
