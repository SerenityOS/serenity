#!/usr/bin/env -S bash ../.port_include.sh
port='rubberband'
version='4.0.0'
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
    "https://breakfastquay.com/files/releases/rubberband-${version}.tar.bz2#af050313ee63bc18b35b2e064e5dce05b276aaf6d1aa2b8a82ced1fe2f8028e9"
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
