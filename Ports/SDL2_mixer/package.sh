#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_mixer'
version='2.6.2'
useconfigure='true'
files=(
    "https://github.com/libsdl-org/SDL_mixer/releases/download/release-${version}/SDL2_mixer-${version}.tar.gz#8cdea810366decba3c33d32b8071bccd1c309b2499a54946d92b48e6922aa371"
)
depends=("libmodplug" "libmpg123" "libvorbis" "SDL2" "timidity")

configure() {
    export LIBS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib"
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --with-sdl-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --enable-music-opus=false \
        --enable-music-opus-shared=false \
        --disable-static \
        --enable-shared \
        EXTRA_LDFLAGS="-lgui -lgfx -lipc -lcore -lcompression"
}

post_configure() {
    unset LIBS
}

build() {
    run make -k
}
