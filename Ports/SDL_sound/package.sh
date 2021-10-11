#!/usr/bin/env -S bash ../.port_include.sh
port=SDL_sound
version=git
_commit=9dfd90e9aebeb8c29248af673a46507f10e0e893
workdir=SDL_sound-${_commit}
useconfigure=true
depends=("SDL2")
files="https://github.com/icculus/SDL_sound/archive/${_commit}.zip ${_commit}.zip c701f31fcef9238d6a439d94020ce8957aa5aaea29878312dc0b6d1c247d77ca"
auth_type=sha256
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
