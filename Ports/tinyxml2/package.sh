#!/usr/bin/env -S bash ../.port_include.sh
port='tinyxml2'
version='11.0.0'
files=(
    "https://github.com/leethomason/tinyxml2/archive/refs/tags/${version}.tar.gz#5556deb5081fb246ee92afae73efd943c889cef0cafea92b0b82422d6a18f289"
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
