#!/usr/bin/env -S bash ../.port_include.sh
port='flare-engine'
useconfigure='true'
version='1.14'
depends=(
    'SDL2'
    'SDL2_image'
    'SDL2_mixer'
    'SDL2_ttf'
)
files=(
    "https://github.com/flareteam/flare-engine/archive/refs/tags/v${version}.tar.gz#2c1bafeaa5ac26c10449bfcb7151b06e8a22547aa7364d2a39bbcb135aa50a09"
)

configopts=(
    "-DCMAKE_INSTALL_PREFIX=${SERENITY_INSTALL_ROOT}/usr/local"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DSDL2_INCLUDE_DIR=${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make "${installopts[@]}" install
}

launcher_name='Flare'
launcher_category='&Games'
launcher_command='/usr/local/games/flare'
icon_file='distribution/flare_logo_icon.png'
