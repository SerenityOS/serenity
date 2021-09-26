#!/usr/bin/env -S bash ../.port_include.sh
port=scummvm
useconfigure="true"
version="2.2.0"
files="https://downloads.scummvm.org/frs/scummvm/${version}/scummvm-${version}.tar.gz scummvm-${version}.tar.gz 6ec5bd63b73861c10ca9869f27a74989a9ad6013bad30a1ef70de6ec146c2cb5"
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

export FREETYPE2_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/freetype2"
export FREETYPE2_LIBS="-lfreetype"
