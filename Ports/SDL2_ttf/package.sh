#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_ttf'
version='2.20.2'
useconfigure='true'
files=(
    "https://github.com/libsdl-org/SDL_ttf/releases/download/release-${version}/SDL2_ttf-${version}.tar.gz#9dc71ed93487521b107a2c4a9ca6bf43fb62f6bddd5c26b055e6b91418a22053"
)
depends=(
    'freetype'
    'SDL2'
)

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --with-sdl-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-x='no' \
        --disable-static \
        --enable-shared \
        FT2_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/freetype2" \
        LIBS='-lgui -lgfx -lipc -lcore -lcoreminimal -lcompress'
}
