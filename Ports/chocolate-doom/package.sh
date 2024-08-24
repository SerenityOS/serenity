#!/usr/bin/env -S bash ../.port_include.sh
port='chocolate-doom'
version='3.1.0'
workdir="${port}-${port}-${version}"
useconfigure='true'
files=(
    "https://github.com/${port}/${port}/archive/refs/tags/${port}-${version}.tar.gz#f2c64843dcec312032b180c3b2f34b4cb26c4dcdaa7375a1601a3b1df11ef84d"
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

configure() {
    run ./autogen.sh --host=x86_64-pc-serenity
}
