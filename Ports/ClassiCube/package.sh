#!/usr/bin/env -S bash ../.port_include.sh

port='ClassiCube'
version='1.3.8'
files=(
    "https://github.com/UnknownShadow200/ClassiCube/archive/refs/tags/${version}.tar.gz#35293acf1e63baeca832dec2512283f2975c79ddf80cc855a12c10464723a6c4"
)
workdir="${port}-${version}/"
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
