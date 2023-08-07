#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_ttf'
version='2.20.1'
useconfigure='true'
files=(
    "https://github.com/libsdl-org/SDL_ttf/releases/download/release-${version}/SDL2_ttf-${version}.tar.gz 78cdad51f3cc3ada6932b1bb6e914b33798ab970a1e817763f22ddbfd97d0c57"
)
depends=("SDL2" "freetype")

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --with-sdl-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-x=no \
        --disable-static \
        --enable-shared \
        FT2_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/freetype2" \
        LIBS="-lgui -lgfx -lipc -lcore -lcompress"
}
