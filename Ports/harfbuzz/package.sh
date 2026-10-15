#!/usr/bin/env -S bash ../.port_include.sh
port='harfbuzz'
version='14.6.0'
files=(
    "https://github.com/harfbuzz/harfbuzz/releases/download/${version}/harfbuzz-${version}.tar.xz#d07a007327277708a2a73ae437887cdbaf282937f6d03ca5467723e9099af586"
)
useconfigure='true'
depends=("freetype" "libicu")
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DBUILD_SHARED_LIBS=ON'
    '-DHB_HAVE_FREETYPE=ON'
    '-DHB_HAVE_ICU=ON'
)

configure() {
    run mkdir -p build
    run cmake -S . -B build "${configopts[@]}"
}

build() {
    run sh -c "cd build && make ${makeopts[@]}"
}

install() {
    run sh -c "cd build && make install"
}
