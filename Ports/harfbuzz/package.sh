#!/usr/bin/env -S bash ../.port_include.sh
port='harfbuzz'
version='4.3.0'
files=(
    "https://github.com/harfbuzz/harfbuzz/releases/download/${version}/harfbuzz-${version}.tar.xz a49628f4c4c8e6d8df95ef44935a93446cf2e46366915b0e3ca30df21fffb530"
)
useconfigure='true'
depends=("freetype" "libicu")
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DHB_HAVE_FREETYPE=ON'
    '-DHB_HAVE_ICU=ON'
    '-DCMAKE_C_FLAGS=-lfreetype'
)

configure() {
    run mkdir -p build
    run sh -c "cd build && cmake .. ${configopts[@]}"
}

build() {
    run sh -c "cd build && make ${makeopts[@]}"
}

install() {
    run sh -c "cd build && make install"
}
