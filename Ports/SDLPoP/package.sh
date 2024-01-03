#!/usr/bin/env -S bash ../.port_include.sh
port='SDLPoP'
useconfigure='true'
version='git'
depends=(
    'SDL2'
    'SDL2_image'
)
commitid='86988c668eeaa10f218e1d4938fc5b4e42314d68'
workdir="${port}-${commitid}"
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
files=(
    "https://github.com/NagyD/SDLPoP/archive/${commitid}.zip#d18cae8541fb8cbcc374fd998316993d561429a83f92061bc0754337ada774c5"
)
launcher_name='Prince of Persia'
launcher_category='&Games'
launcher_command='/opt/PrinceOfPersia/prince'
icon_file='src/icon.ico'

configure() {
    run cmake "${configopts[@]}" ./src
}

install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/opt/PrinceOfPersia"
    run cp -r prince data SDLPoP.ini "${SERENITY_INSTALL_ROOT}/opt/PrinceOfPersia" 
}
