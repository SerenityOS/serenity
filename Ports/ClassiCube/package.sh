#!/usr/bin/env -S bash ../.port_include.sh

port='ClassiCube'
version='1.3.9'
files=(
    "https://github.com/UnknownShadow200/ClassiCube/archive/refs/tags/${version}.tar.gz#5bb62dbb3ad27e1fbb09842f0cb1f4802b2686553b017e0b67ce09b36f908246"
)
depends=(
    'SDL2'
    'curl'
    'libopenal'
)
launcher_name='ClassiCube'
launcher_category='&Games'
launcher_workdir='/home/anon/Games/ClassiCube'
launcher_command='/usr/local/bin/ClassiCube'
icon_file='misc/CCicon.ico'

makeopts+=(
    'PLAT=serenityos'
)

install() {
    run mkdir -p ${SERENITY_INSTALL_ROOT}/home/anon/Games/ClassiCube
    run cp ClassiCube ${SERENITY_INSTALL_ROOT}/usr/local/bin
}
