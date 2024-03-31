#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_mixer'
version='2.6.3'
useconfigure='true'
files=(
    "https://github.com/libsdl-org/SDL_mixer/releases/download/release-${version}/SDL2_mixer-${version}.tar.gz#7a6ba86a478648ce617e3a5e9277181bc67f7ce9876605eea6affd4a0d6eea8f"
)
depends=(
    'libmodplug'
    'libmpg123'
    'libvorbis'
    'SDL2'
    'timidity'
)

configure() {
    export LIBS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib"
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --with-sdl-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --enable-music-opus='false' \
        --enable-music-opus-shared='false' \
        --disable-static \
        --enable-shared \
        EXTRA_LDFLAGS='-lgui -lgfx -lipc -lcore -lcoreminimal -lcompression'
}

post_configure() {
    unset LIBS
}

build() {
    run make -k
}
