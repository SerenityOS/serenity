#!/usr/bin/env -S bash ../.port_include.sh
port=freeciv
version=3.0.0-beta2
useconfigure=true
use_fresh_config_sub=true
configopts=("--enable-client=sdl2" "--enable-fcmp=no")
files="http://files.freeciv.org/beta/freeciv-${version}.tar.xz freeciv-${version}.tar.xz 9f44dc28068b239c18bda68192ddb27622030880f8ab9c17f777eac28391269e"
auth_type=sha256
depends=("SDL2" "SDL2_image" "SDL2_mixer" "SDL2_ttf" "SDL2_gfx" "zstd" "libicu" "xz" "gettext" "curl")
launcher_name=Freeciv
launcher_category=Games
launcher_command=/usr/local/bin/freeciv-sdl2
icon_file=windows/client.ico
