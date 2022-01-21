#!/usr/bin/env -S bash ../.port_include.sh
port=SDL2_mixer
version=2.0.4
useconfigure=true
use_fresh_config_sub=true
config_sub_path=build-scripts/config.sub
files="https://www.libsdl.org/projects/SDL_mixer/release/SDL2_mixer-${version}.tar.gz SDL2_mixer-${version}.tar.gz b4cf5a382c061cd75081cf246c2aa2f9df8db04bdda8dcdc6b6cca55bede2419"
auth_type=sha256
depends=("libmodplug" "libvorbis" "SDL2")

configure() {
    export LIBS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib"
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --with-sdl-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --enable-music-opus=false \
        --enable-music-opus-shared=false \
        EXTRA_LDFLAGS="-lgui -lgfx -lipc -lcore -lcompression"
}

post_configure() {
    unset LIBS
}

build() {
    run make -k
}

install() {
    run make -k DESTDIR="${SERENITY_INSTALL_ROOT}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libSDL2_mixer.so -Wl,-soname,libSDL2_mixer.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libSDL2_mixer.a -Wl,--no-whole-archive -Wl,--no-as-needed -lvorbis -lvorbisfile
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libSDL2_mixer.la
}
