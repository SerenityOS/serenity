#!/usr/bin/env -S bash ../.port_include.sh
port='cavestory'
version='2.6.5-1'
files=('https://github.com/gloof11/nxengine-evo/archive/0f45555c64bae21116bac30cd46002e70b1b6495.zip nxengine-evo-0f45555c64bae21116bac30cd46002e70b1b6495.zip c93cb2c1e16f49cd87bcc886cf6adf289355fabe7b5a30d506ec066cc1d86d1d')
depends=(
    'libjpeg'
    'libpng'
    'SDL2'
    'SDL2_image'
    'SDL2_mixer'
    'SDL2_ttf'
)
workdir="nxengine-evo-0f45555c64bae21116bac30cd46002e70b1b6495"
launcher_name='Cave Story'
launcher_category='Games'
launcher_command='/usr/local/bin/nxengine-evo'
icon_file='platform/switch/icon.jpg'
useconfigure='true'
configopts=(
    '-DCMAKE_BUILD_TYPE=Release ..'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DSDL2_INCLUDE_DIR=${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
    "-DSDL2_IMAGE_INCLUDE_DIR=${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
    "-DSDL2_MIXER_INCLUDE_DIR=${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
)

configure() {
    run cmake -B build "${configopts[@]}"
}

build () {
    run make -C build "${makeopts[@]}"
}

install () {
    run make -C build install
}
