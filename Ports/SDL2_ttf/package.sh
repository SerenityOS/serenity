#!/usr/bin/env -S bash ../.port_include.sh
port=SDL2_ttf
version=2.0.15
useconfigure=true
files="https://www.libsdl.org/projects/SDL_ttf/release/SDL2_ttf-${version}.tar.gz SDL2_ttf-${version}.tar.gz 04fe06ff7623d7bdcb704e82f5f88391"
auth_type=md5
depends="SDL2 freetype"

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --with-sdl-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-x=no \
        FT2_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/freetype2" \
        LIBS="-lgui -lgfx -lipc -lcore -lcompress"
}

install() {
    run make install DESTDIR=${SERENITY_INSTALL_ROOT} $installopts
    run ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libSDL2_ttf.so -Wl,-soname,libSDL2_ttf.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libSDL2_ttf.a -Wl,--no-whole-archive
}
