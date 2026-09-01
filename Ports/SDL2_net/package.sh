#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_net'
version='2.4.0'
useconfigure='true'
configopts=(
    "--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "--disable-static"
    "--enable-shared"
)
files=(
    "https://github.com/libsdl-org/SDL_net/releases/download/release-${version}/SDL2_net-${version}.tar.gz#9cbca2527feb3f1a622d48ba65cc7dee9b1e3f2c55ceafb7d7720bb058aafb30"
)
depends=("SDL2")
