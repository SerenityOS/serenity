#!/usr/bin/env -S bash ../.port_include.sh
port='VVVVVV'
version='2.3.6'
useconfigure='true'
files=(
    "https://github.com/TerryCavanagh/VVVVVV/archive/refs/tags/${version}.tar.gz#a3366aab9e8462d330044ab1ec63927e9f5c3801c0ed96b24f08c553dcb911e9"
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_BUILD_TYPE=Release"
    "-Sdesktop_version"
)
depends=("SDL2" "SDL2_mixer")
icon_file='desktop_version/icon.ico'
launcher_name='VVVVVV'
launcher_category='&Games'
launcher_command='/opt/VVVVVV/VVVVVV'

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/opt/VVVVVV"
    run cp VVVVVV "${SERENITY_INSTALL_ROOT}/opt/VVVVVV"
    echo -e "\033[0;34m=====================================================================\033[0m"
    echo -e "\033[1;31mNOTE: \033[0mVVVVVV needs the assets from the original game to work."
    echo -e "Place the \033[1;33mdata.zip\033[0m file from the Make and Play edition AS IS"
    echo -e "under \033[1;32m${SERENITY_INSTALL_ROOT}/opt/VVVVVV\033[0m."
    echo -e "\033[0;34m=====================================================================\033[0m"
}
