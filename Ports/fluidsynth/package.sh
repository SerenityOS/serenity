#!/usr/bin/env -S bash ../.port_include.sh
port='fluidsynth'
version='2.6.2'
useconfigure='true'
configopts=(
    '-G Ninja'
    "-S fluidsynth-${version}"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
files=(
    "https://github.com/FluidSynth/fluidsynth/archive/refs/tags/v${version}.tar.gz#ca376b42a3c1b186a4c8c248931789a455ac94a89b777394969d678010c2f91f"
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
