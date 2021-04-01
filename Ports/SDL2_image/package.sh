#!/usr/bin/env -S bash ../.port_include.sh
port=SDL2_image
useconfigure=true
version=2.0.5
depends="SDL2 libpng libjpeg"
files="https://www.libsdl.org/projects/SDL_image/release/SDL2_image-${version}.tar.gz SDL_image-${version}.tar.gz"

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --with-sdl-prefix="${SERENITY_BUILD_DIR}/Root/usr" \
        --prefix="/usr"                                    \
        --enable-webp=false --enable-webp-shared=false     \
        LDFLAGS="-lgui -lgfx -lipc -lcore -lm"
}

build() {
    run make -k
}

install() {
    run make -k DESTDIR="${SERENITY_BUILD_DIR}/Root" install
}
