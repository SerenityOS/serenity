#!/usr/bin/env -S bash ../.port_include.sh
port=SDL_sound
version=git
workdir=SDL_sound-main
useconfigure=true
deps="SDL2"
files="https://github.com/icculus/SDL_sound/archive/refs/heads/main.zip main.zip f8a322d090a172b9c66a41758f7ece850a8ff231058733a13e44bc380342651b"
auth_type=sha256
configopts="-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"

configure() {
    run cmake $configopts
}

install() {
    run make install
}
