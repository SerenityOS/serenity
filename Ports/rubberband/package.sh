#!/usr/bin/env -S bash ../.port_include.sh
port='rubberband'
version='3.3.0'
depends=(
    'libfftw3'
    'libopus'
    'libsamplerate'
    'libsndfile'
)
useconfigure='true'
configopts=(
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    '-Dfft=fftw'
    '-Dresampler=libsamplerate'
    '-Djni=disabled'
    '-Dvamp=disabled'
    '-Dladspa=disabled'
    '-Dlv2=disabled'
    '-Dtests=disabled'
    '-Ddefault_library=shared'
)
files=(
    "https://breakfastquay.com/files/releases/rubberband-${version}.tar.bz2#d9ef89e2b8ef9f85b13ac3c2faec30e20acf2c9f3a9c8c45ce637f2bc95e576c"
)

configure() {
    run meson setup build "${configopts[@]}"
}

build() {
    run ninja -C build
}

install() {
    export DESTDIR="${SERENITY_INSTALL_ROOT}"
    run ninja -C build install
}
