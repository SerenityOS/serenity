#!/usr/bin/env -S bash ../.port_include.sh
port='sdl12-compat'
version='1.2.78'
workdir="sdl12-compat-release-${version}"
useconfigure='true'
files=(
    "https://github.com/libsdl-org/sdl12-compat/archive/refs/tags/release-${version}.tar.gz#40ec5f0bab13a217ffae7aab0c450f1f798761e91fb185051b5211925c9da11a"
)
depends=(
    'glu'
    'SDL2'
)

configure() {
    run cmake \
        -B 'build' \
        -DCMAKE_TOOLCHAIN_FILE="${SERENITY_BUILD_DIR}/CMakeToolchain.txt" \
        -DCMAKE_BUILD_TYPE='Release'
}

build() {
    (
        cd ${workdir}/build/
        make "${makeopts[@]}"
    )
}

install() {
    (
        cd ${workdir}/build/
        make install
    )
}
