#!/usr/bin/env -S bash ../.port_include.sh
port='prboom-plus'
version='2.6.66'
useconfigure='true'
squashed_version="$(echo ${version} | tr -d '.')"
files=(
    "https://github.com/coelckers/prboom-plus/archive/refs/tags/v${version}.tar.gz#4b647b4b14c3fac00711e6bf19f996bbfe37754a3b9bb5be6791f0c3fd993438"
    "https://github.com/coelckers/prboom-plus/releases/download/v${version}/prboom-plus-${squashed_version}-ucrt64.zip#9c8c2b021a57739e8428b599d80c25a6b95150084cb04a7e433877123b15b141"
)
workdir="prboom-plus-${version}/prboom2"
depends=(
    'glu'
    'libmad'
    'libvorbis'
    'SDL2'
    'SDL2_image'
    'SDL2_mixer'
    'SDL2_net'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    # Upstream declares 3.0 which is no longer supported by our CMake.
    '-DCMAKE_POLICY_VERSION_MINIMUM=3.25'
    '-DCMAKE_C_STANDARD=17'
    '-DCMAKE_C_FLAGS=-Wno-error=incompatible-pointer-types'
    '-DWITH_ALSA=OFF'
    '-DWITH_DUMB=OFF'
    '-DWITH_FLUIDSYNTH=OFF'
    '-DWITH_PCRE=OFF'
    '-DWITH_PORTMIDI=OFF'
    '-Wno-dev'
)

launcher_name='PrBoom+'
launcher_category='&Games'
launcher_command='/usr/local/bin/prboom-plus -vidmode gl'
icon_file='ICONS/prboom-plus.ico'

configure() {
    run cmake -B build "${configopts[@]}"
}

build() {
    run make -C build "${makeopts[@]}"
}

install() {
    run make -C build install

    wad_directory="${SERENITY_INSTALL_ROOT}/usr/local/share/games/doom"
    mkdir -p "${wad_directory}"
    run cp "../../prboom-plus-${squashed_version}-ucrt64/prboom-plus.wad" "${wad_directory}/"
}
