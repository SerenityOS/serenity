#!/usr/bin/env -S bash ../.port_include.sh

port='ClassiCube'
version='1.3.3'
files=(
    "https://github.com/UnknownShadow200/ClassiCube/archive/refs/tags/${version}.tar.gz ${version}.tar.gz f90acfeb82fd440ead6e086694d99bd1583b0174da1801687c4c3d0fcb21d83d"
)
workdir="${port}-${version}/src/"
depends=(
    'SDL2'
    'curl'
    'libopenal'
)
launcher_name='ClassiCube'
launcher_category='Games'
launcher_workdir='/home/anon/Games/ClassiCube'
launcher_command='/usr/local/bin/ClassiCube'

makeopts+=(
    'ClassiCube'
    'PLAT=serenity'
)

install() {
    run mkdir -p ${SERENITY_INSTALL_ROOT}/home/anon/Games/ClassiCube
    run cp ClassiCube ${SERENITY_INSTALL_ROOT}/usr/local/bin
}
