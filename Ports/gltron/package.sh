#!/usr/bin/env -S bash ../.port_include.sh
port='gltron'
useconfigure='true'
use_fresh_config_sub='true'
version='0.70'
files=(
    "http://mirror.sobukus.de/files/grimoire/games-arcade-2d/gltron-${version}-source.tar.gz#e0c8ebb41a18a1f8d7302a9c2cb466f5b1dd63e9a9966c769075e6b6bdad8bb0"
)
depends=("libpng" "glu" "SDL_sound" "sdl12-compat" "zlib")
configopts=(
    "--disable-warn"
    "--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "LIBS=-lm -lSDL_sound"
)
launcher_name='GLTron'
launcher_category='&Games'
launcher_command='/usr/local/bin/gltron'
icon_file='art/default/gltron.png'
