#!/usr/bin/env -S bash ../.port_include.sh
port=gltron
useconfigure="true"
use_fresh_config_sub=true
version="0.70"
files="http://mirror.sobukus.de/files/grimoire/games-arcade-2d/gltron-${version}-source.tar.gz gltron-${version}-source.tar.gz e0c8ebb41a18a1f8d7302a9c2cb466f5b1dd63e9a9966c769075e6b6bdad8bb0"
auth_type=sha256
depends=("libpng" "SDL_sound" "SDL2" "zlib")
configopts=(
    "--disable-warn"
)

launcher_name="GLTron"
launcher_category="Games"
launcher_command="/usr/local/bin/gltron"
icon_file="art/default/gltron.png"

pre_configure() {
    export CPPFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/include/LibGL -I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
    export LIBS="-lSDL2_sound"
    export SDL_CONFIG="${SERENITY_INSTALL_ROOT}/usr/local/bin/sdl2-config"
}

post_configure() {
    unset CPPFLAGS
    unset LIBS
    unset SDL_CONFIG
}
