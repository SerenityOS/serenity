#!/usr/bin/env -S bash ../.port_include.sh
port='sdl12-compat'
version='1.2.76'
workdir="sdl12-compat-release-${version}"
useconfigure='true'
files=(
    "https://github.com/libsdl-org/sdl12-compat/archive/refs/tags/release-${version}.tar.gz#e889ac9c7e8a6bdfc31972bf1f1254b84882cb52931608bada62e8febbf0270b"
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
