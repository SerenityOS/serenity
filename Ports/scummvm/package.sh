#!/usr/bin/env -S bash ../.port_include.sh
port=scummvm
useconfigure="true"
version="2.5.0"
files="https://downloads.scummvm.org/frs/scummvm/${version}/scummvm-${version}.tar.xz scummvm-${version}.tar.xz b47ee4b195828d2c358e38a4088eda49886dc37a04f1cc17b981345a59e0d623"
auth_type=sha256
depends=("freetype" "libiconv" "libjpeg" "libpng" "libtheora" "SDL2")
configopts=(
    "--enable-c++11"
    "--enable-release-mode"
    "--enable-optimizations"
    "--opengl-mode=none"
    "--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
)
launcher_name=ScummVM
launcher_category=Games
launcher_command=/usr/local/bin/scummvm
icon_file=icons/scummvm.ico

function pre_configure() {
    export CPPFLAGS="-fvisibility=hidden"
    export FREETYPE2_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/freetype2"
    export SDL_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
}

function post_configure() {
    unset CPPFLAGS
    unset FREETYPE2_CFLAGS
    unset SDL_CFLAGS
}
