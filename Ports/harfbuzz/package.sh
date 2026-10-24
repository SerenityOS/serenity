#!/usr/bin/env -S bash ../.port_include.sh
port='harfbuzz'
version='15.0.0'
files=(
    "https://github.com/harfbuzz/harfbuzz/releases/download/${version}/harfbuzz-${version}.tar.xz#968536e9abd782f07f7954044c0c4020199fcdd39b41d221ec42d2a3be4a44df"
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
