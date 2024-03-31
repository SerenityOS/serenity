#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_image'
version='2.6.3'
useconfigure='true'
depends=(
    'libjpeg'
    'libpng'
    'libtiff'
    'SDL2'
)
files=(
    "https://github.com/libsdl-org/SDL_image/releases/download/release-${version}/SDL2_image-${version}.tar.gz#931c9be5bf1d7c8fae9b7dc157828b7eee874e23c7f24b44ba7eff6b4836312c"
)
configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
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
