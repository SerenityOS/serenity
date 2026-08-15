#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_sound'
version='301135a6d0d9bb77c9da0b7f809e9a10d579610f'
workdir="SDL_sound-${version}"
useconfigure='true'
depends=('SDL2')
files=(
    "https://github.com/icculus/SDL_sound/archive/${version}.zip#d29f90dd5abacf9f818f0b1567fab6b3dc6292d0a942e8e8d1e8f84130eea7a1"
)
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
