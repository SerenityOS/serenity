#!/usr/bin/env -S bash ../.port_include.sh
port='chocolate-doom'
version='3.1.1'
useconfigure='true'
workdir="${port}-${port}-${version}"
files=(
    "https://github.com/chocolate-doom/chocolate-doom/archive/refs/tags/chocolate-doom-${version}.tar.gz#1edcc41254bdc194beb0d33e267fae306556c4d24110a1d3d3f865717f25da23"
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

pre_configure() {
    run autoreconf -i
}
