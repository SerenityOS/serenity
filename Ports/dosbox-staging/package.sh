#!/usr/bin/env -S bash ../.port_include.sh
port='dosbox-staging'
version='0.77.1'
useconfigure='true'
configopts=(
    "--cross-file" "${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    '-Ddynamic_core=none'
    '-Dtry_static_libs=opusfile'
    '-Dunit_tests=disabled'
    '-Duse_fluidsynth=false'
    '-Duse_mt32emu=false'
    '-Duse_opengl=false'
    '-Duse_png=false'
)
files=(
    "https://github.com/dosbox-staging/dosbox-staging/archive/refs/tags/v${version}.tar.gz v${version}.tar.gz 85359efb7cd5c5c0336d88bdf023b7b462a8233490e00274fef0b85cca2f5f3c"
)
depends=(
    'libslirp'
    'libpng'
    'opusfile'
    'SDL2'
    'SDL2_net'
)
launcher_name='DOSBox'
launcher_category='Games'
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
