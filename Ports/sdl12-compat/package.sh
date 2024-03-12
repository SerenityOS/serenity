#!/usr/bin/env -S bash ../.port_include.sh
port='sdl12-compat'
version='1.2.64'
workdir="sdl12-compat-release-${version}"
useconfigure='true'
files=(
    "https://github.com/libsdl-org/sdl12-compat/archive/refs/tags/release-${version}.tar.gz#3e308e817c7f0c6383225485e9a67bf1119ad684b8cc519038671cc1b5d29861"
)
depends=(
    'glu'
    'SDL2'
)

configure() {
    run cmake \
        -B 'build' \
        -DCMAKE_TOOLCHAIN_FILE="${SERENITY_BUILD_DIR}/CMakeToolchain.txt" \
        -DCMAKE_BUILD_TYPE='Release' \
        -DCMAKE_INSTALL_PREFIX="${SERENITY_INSTALL_ROOT}/usr/local/"
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
