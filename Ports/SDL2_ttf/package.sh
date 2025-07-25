#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_ttf'
version='2.24.0'
useconfigure='true'
files=(
    "https://github.com/libsdl-org/SDL_ttf/releases/download/release-${version}/SDL2_ttf-${version}.tar.gz#0b2bf1e7b6568adbdbc9bb924643f79d9dedafe061fa1ed687d1d9ac4e453bfd"
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
