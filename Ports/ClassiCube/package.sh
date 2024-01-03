#!/usr/bin/env -S bash ../.port_include.sh

port='ClassiCube'
version='1.3.6'
files=(
    "https://github.com/UnknownShadow200/ClassiCube/archive/refs/tags/${version}.tar.gz#fab780f4dcf0669a0f94683c9b6596f40cb83e09727a3b91aaae5e934a9740b0"
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
