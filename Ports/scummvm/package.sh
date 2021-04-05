#!/usr/bin/env -S bash ../.port_include.sh
port=scummvm
useconfigure="true"
version="2.2.0"
files="https://downloads.scummvm.org/frs/scummvm/${version}/scummvm-${version}.tar.gz scummvm-${version}.tar.gz"
depends="SDL2"
# Optionally depends (you probably want to install):
#   libogg libvorbis flac libmad libjpeg libpng libtheora
#   faad2 zlib libmpeg2 readline freetype fribidi nasm curl

configure() {
    export LIBS="-lgui -lgfx -lcore"
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --prefix=/usr                         \
        --opengl-mode=none                    \
        --with-sdl-prefix="${SERENITY_BUILD_DIR}/Root/usr"
}
