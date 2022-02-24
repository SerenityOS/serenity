#!/usr/bin/env -S bash ../.port_include.sh
port=scummvm
useconfigure="true"
version="2.5.1"
files="https://downloads.scummvm.org/frs/scummvm/${version}/scummvm-${version}.tar.xz scummvm-${version}.tar.xz 9fd8db38e4456144bf8c34dacdf7f204e75f18e8e448ec01ce08ce826a035f01"
auth_type=sha256
depends=("freetype" "libiconv" "libjpeg" "libmad" "libmpeg2" "libpng" "libtheora" "SDL2")
configopts=(
    "--enable-c++11"
    "--enable-engine=monkey4"
    "--enable-optimizations"
    "--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
)
launcher_name=ScummVM
launcher_category=Games
launcher_command=/usr/local/bin/scummvm
icon_file=icons/scummvm.ico

function pre_configure() {
    export CPPFLAGS="-fvisibility=hidden"
    export FREETYPE2_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/freetype2"
    export OPENGL_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/include/LibGL"
    export SDL_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
}

function post_configure() {
    unset CPPFLAGS
    unset FREETYPE2_CFLAGS
    unset OPENGL_CFLAGS
    unset SDL_CFLAGS
}
