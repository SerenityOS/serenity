#!/usr/bin/env -S bash ../.port_include.sh
port='pacman'
version='b6241a373cc09f021c2ab29714eca5f9e33463f8'
files=(
    "https://github.com/ebuc99/pacman/archive/${version}.zip#d688f75d33c7bf4f217bfcaf0d5ee507fd73bb233d77303927d15b54988f0231"
)
useconfigure='true'
depends=(
    'SDL2'
    'SDL2_image'
    'SDL2_ttf'
    'SDL2_mixer'
)
configopts=(
    "--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
)
use_fresh_config_sub='true'
config_sub_paths=('config.sub')
launcher_name='Pacman'
launcher_category='&Games'
launcher_command='/usr/local/bin/pacman'
icon_file='data/gfx/pacman_desktop.png'

pre_patch() {
    run ./autogen.sh
}
