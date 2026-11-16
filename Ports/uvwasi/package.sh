#!/usr/bin/env -S bash ../.port_include.sh
port='uvwasi'
version='20a4f653'
workdir='uvwasi-20a4f6534616f7b585425a708d495bb8d6f0b6b7'
useconfigure='true'
files=(
    "https://github.com/nodejs/uvwasi/archive/${version}.tar.gz#271786e0462273b0c9767154da1fdc6b18bf6673673810037fff1823391f7ae4"
)
configopts=(
    '-Bbuild'
    '-DCMAKE_BUILD_TYPE=Release'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
depends=(
    libuv
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
