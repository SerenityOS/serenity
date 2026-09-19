#!/usr/bin/env -S bash ../.port_include.sh
port='fluidsynth'
version='2.6.1'
useconfigure='true'
configopts=(
    '-G Ninja'
    "-S fluidsynth-${version}"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
files=(
    "https://github.com/FluidSynth/fluidsynth/archive/refs/tags/v${version}.tar.gz#3d258a3bf97cc20c59eeebfe62c2432fae88adda74d3ad098681c76e0ebf446b"
)
depends=(
    'glib'
    'SDL2'
)

configure() {
    cmake \
        "${configopts[@]}" \
        -B fluidsynth-build
}

build() {
    ninja -C fluidsynth-build
}

install() {
    ninja -C fluidsynth-build install
}
