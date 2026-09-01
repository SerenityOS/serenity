#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_image'
version='2.8.12'
useconfigure='true'
depends=(
    'libjpeg'
    'libpng'
    'libtiff'
    'SDL2'
)
files=(
    "https://github.com/libsdl-org/SDL_image/releases/download/release-${version}/SDL2_image-${version}.tar.gz#393f5efb50536ec13ca4f4affb69cc9966d3c3f969e6c5e701faddf9f9785381"
)
configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-serenity" \
        --with-sdl-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --enable-webp='false' \
        --enable-webp-shared='false' \
        --disable-static \
        --enable-shared \
        LDFLAGS='-lgui -lgfx -lipc -lcore -lcoreminimal -lm'
}

build() {
    run make -k
}
