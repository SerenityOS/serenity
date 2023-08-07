#!/usr/bin/env -S bash ../.port_include.sh
port=freeciv
version=3.0.8
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("bootstrap/config.sub")
configopts=("--enable-client=sdl2" "--enable-fcmp=no")
files=(
    "http://files.freeciv.org/stable/freeciv-${version}.tar.xz 3b5aa32f628890be1741c3ac942cee82c79c065f8db6baff18d734a5c0e776d4"
)
depends=("SDL2" "SDL2_image" "SDL2_mixer" "SDL2_ttf" "SDL2_gfx" "zstd" "libicu" "xz" "gettext" "curl")
launcher_name=Freeciv
launcher_category=Games
launcher_command=/usr/local/bin/freeciv-sdl2
icon_file=windows/client.ico
