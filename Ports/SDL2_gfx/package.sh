#!/usr/bin/env -S bash ../.port_include.sh

port=SDL2_gfx
version=1.0.4
files="https://downloads.sourceforge.net/project/sdl2gfx/SDL2_gfx-${version}.tar.gz SDL2_gfx-${version}.tar.gz 15f9866c6464ca298f28f62fe5b36d9f"
auth_type=md5
depends="SDL2"
useconfigure=true
configopts="--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local"

install() {
    run make install DESTDIR=${SERENITY_INSTALL_ROOT} $installopts
    run ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libSDL2_gfx.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libSDL2_gfx.a -Wl,--no-whole-archive
}
