#!/usr/bin/env -S bash ../.port_include.sh
port=SDL2_net
version=2.0.1
useconfigure=true
use_fresh_config_sub=true
configopts=("--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local")
files="https://www.libsdl.org/projects/SDL_net/release/SDL2_net-${version}.tar.gz SDL2_net-${version}.tar.gz 15ce8a7e5a23dafe8177c8df6e6c79b6749a03fff1e8196742d3571657609d21"
auth_type=sha256
depends=("SDL2")

post_install() {
    run ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libSDL2_net.so -Wl,-soname,libSDL2_net.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libSDL2_net.a -Wl,--no-whole-archive
}
