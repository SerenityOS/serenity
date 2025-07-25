#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_image'
version='2.8.8'
useconfigure='true'
depends=(
    'libjpeg'
    'libpng'
    'libtiff'
    'SDL2'
)
files=(
    "https://github.com/libsdl-org/SDL_image/releases/download/release-${version}/SDL2_image-${version}.tar.gz#2213b56fdaff2220d0e38c8e420cbe1a83c87374190cba8c70af2156097ce30a"
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
