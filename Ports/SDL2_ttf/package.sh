#!/usr/bin/env -S bash ../.port_include.sh
port=SDL2_ttf
version=2.0.15
useconfigure=true
files="https://www.libsdl.org/projects/SDL_ttf/release/SDL2_ttf-${version}.tar.gz SDL2_ttf-${version}.tar.gz"
depends="SDL2 freetype"

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --with-sdl-prefix="${SERENITY_BUILD_DIR}/Root/usr" \
        LIBS="-lgui -lgfx -lipc -lcore -lcompress"
}
