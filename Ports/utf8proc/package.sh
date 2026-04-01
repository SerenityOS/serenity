#!/usr/bin/env -S bash ../.port_include.sh
port='utf8proc'
version='2.11.3'
files=(
    "https://github.com/JuliaStrings/utf8proc/releases/download/v${version}/utf8proc-${version}.tar.gz#415189fd2c85cd6ee5ff26af500fa387de9ada1e3e316e93f7338551481d557d"
)
useconfigure='true'

configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
)

configure() {
    run cmake -G Ninja -B build -S . "${configopts[@]}"
}

build() {
    run cmake --build build --parallel "${MAKEJOBS}"
}

install() {
    run cmake --install build
}
