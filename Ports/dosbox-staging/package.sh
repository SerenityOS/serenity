#!/usr/bin/env -S bash ../.port_include.sh
port='dosbox-staging'
version='0.80.1'
useconfigure='true'
configopts=(
    "--cross-file" "${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    '-Ddynamic_core=none'
    '-Dtry_static_libs=opusfile'
    '-Dunit_tests=disabled'
    '-Duse_opengl=false'
)
files=(
    "https://github.com/dosbox-staging/dosbox-staging/archive/refs/tags/v${version}.tar.gz#2ca69e65e6c181197b63388c60487a3bcea804232a28c44c37704e70d49a0392"
)
depends=(
    'fluidsynth'
    'libmt32emu'
    'libslirp'
    'libpng'
    'opusfile'
    'SDL2'
    'SDL2_image'
    'SDL2_net'
)
launcher_name='DOSBox'
launcher_category='&Games'
launcher_command='/usr/local/bin/dosbox'
icon_file='contrib/icons/dosbox-staging.ico'

configure() {
    run meson setup build/release "${configopts[@]}"
}

build() {
    run ninja -C build/release
}

install() {
    export DESTDIR="${SERENITY_INSTALL_ROOT}"
    run meson install -C build/release
}

post_install() {
    echo
    echo "DOSBox Staging ${version} is installed!"
    echo
    echo "Release notes: https://dosbox-staging.github.io/downloads/release-notes/${version}/"
    echo
}
