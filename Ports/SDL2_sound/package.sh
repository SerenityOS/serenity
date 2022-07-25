#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_sound'
version='git'
_commit='df3fc779774c2c5dc1147239da1af858c88f1a74'
workdir="SDL_sound-${_commit}"
useconfigure='true'
depends=('SDL2')
files="https://github.com/icculus/SDL_sound/archive/${_commit}.zip ${_commit}.zip bf655a03ab96a49c4140e19135433d62893c124330955e85e3dfddbe9963bac2"
auth_type='sha256'
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
