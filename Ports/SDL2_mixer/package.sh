#!/usr/bin/env -S bash ../.port_include.sh
port=SDL2_mixer
version=2.0.4
useconfigure=true
files="https://www.libsdl.org/projects/SDL_mixer/release/SDL2_mixer-${version}.tar.gz SDL2_mixer-${version}.tar.gz"
depends="SDL2"

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --with-sdl-prefix="${SERENITY_BUILD_DIR}/Root/usr" \
        --prefix="/usr"                                    \
        --enable-music-opus=false --enable-music-opus-shared=false \
        --enable-music-mod-modplug=false --enable-music-mod-modplug-shared=false \
        EXTRA_LDFLAGS="-lgui -lgfx -lipc -lcore -lcompression"
}

build() {
    run make -k
}

install() {
    run make -k DESTDIR="${SERENITY_BUILD_DIR}/Root" install
}
