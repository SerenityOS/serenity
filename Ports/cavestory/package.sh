#!/usr/bin/env -S bash ../.port_include.sh
port='cavestory'
version='2.6.5-1'
files=(
    'https://github.com/gloof11/nxengine-evo/archive/b427ed7bcd403a4dbb07703fe0eb015c3350bbfc.zip#83e66960e27ec928d1217439754f0dd733765ecaf760c02832e5b35f4858ea8a'
)
depends=(
    'libjpeg'
    'libpng'
    'SDL2'
    'SDL2_image'
    'SDL2_mixer'
    'SDL2_ttf'
)
workdir="nxengine-evo-b427ed7bcd403a4dbb07703fe0eb015c3350bbfc"
launcher_name='Cave Story'
launcher_category='&Games'
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
