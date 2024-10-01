#!/usr/bin/env -S bash ../.port_include.sh
port='fluidsynth'
version='2.3.5'
useconfigure='true'
configopts=(
    '-G Ninja'
    "-S fluidsynth-${version}"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
files=(
    "https://github.com/FluidSynth/fluidsynth/archive/refs/tags/v${version}.tar.gz#f89e8e983ecfb4a5b4f5d8c2b9157ed18d15ed2e36246fa782f18abaea550e0d"
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
