#!/usr/bin/env -S bash ../.port_include.sh
port='sdl12-compat'
version='1.2.56'
workdir=sdl12-compat-release-${version}
useconfigure='true'
files=(
    "https://github.com/libsdl-org/sdl12-compat/archive/refs/tags/release-${version}.tar.gz#f62f3e15f95aade366ee6c03f291e8825c4689390a6be681535259a877259c58"
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_BUILD_TYPE=Release"
    "-DCMAKE_INSTALL_PREFIX=${SERENITY_INSTALL_ROOT}/usr/local/"
    "-B./build"
)
depends=("SDL2")

configure() {
    run cmake "${configopts[@]}"
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
