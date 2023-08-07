#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_image'
useconfigure='true'
version='2.6.2'
depends=("SDL2" "libpng" "libjpeg" "libtiff")
files=(
    "https://github.com/libsdl-org/SDL_image/releases/download/release-${version}/SDL2_image-${version}.tar.gz 48355fb4d8d00bac639cd1c4f4a7661c4afef2c212af60b340e06b7059814777"
)

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --with-sdl-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --enable-webp=false --enable-webp-shared=false     \
        --disable-static \
        --enable-shared \
        LDFLAGS="-lgui -lgfx -lipc -lcore -lm"
}

build() {
    run make -k
}
