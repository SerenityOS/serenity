#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_mixer'
version='2.8.2'
useconfigure='true'
files=(
    "https://github.com/libsdl-org/SDL_mixer/releases/download/release-${version}/SDL2_mixer-${version}.tar.gz#938dff531d00ace2296557a6599abe6f34599e2f34f0a4a08a397e2ccac8b8f7"
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
        --host="${SERENITY_ARCH}-serenity" \
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
