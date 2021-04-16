#!/usr/bin/env -S bash ../.port_include.sh
port=scummvm
useconfigure="true"
version="2.2.0"
files="https://downloads.scummvm.org/frs/scummvm/${version}/scummvm-${version}.tar.gz scummvm-${version}.tar.gz f48f07347e5ab0b3094a868367c0e1f2"
auth_type=md5
depends="SDL2"

configure() {
    export LIBS="-lgui -lgfx -lcore"
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --opengl-mode=none                    \
        --with-sdl-prefix="${SERENITY_BUILD_DIR}/Root/usr/local"
}
