#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2_net'
version='2.2.0'
useconfigure='true'
configopts=(
    "--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "--disable-static"
    "--enable-shared"
)
files=(
    "https://github.com/libsdl-org/SDL_net/releases/download/release-${version}/SDL2_net-${version}.tar.gz#4e4a891988316271974ff4e9585ed1ef729a123d22c08bd473129179dc857feb"
)
depends=("SDL2")
