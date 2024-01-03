#!/usr/bin/env -S bash ../.port_include.sh
port='chocolate-doom'
version='3.0.1'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('autotools/config.sub')
files=(
    "https://www.chocolate-doom.org/downloads/${version}/chocolate-doom-${version}.tar.gz#d435d6177423491d60be706da9f07d3ab4fabf3e077ec2a3fc216e394fcfc8c7"
)
depends=(
    'libpng'
    'libsamplerate'
    'SDL2'
    'SDL2_mixer'
    'SDL2_net'
)

launcher_name='Chocolate Doom'
launcher_category='&Games'
launcher_command='/usr/local/bin/chocolate-doom'
icon_file='data/doom.png'
