#!/usr/bin/env -S bash ../.port_include.sh
port='quake2'
version='0.1'
useconfigure='true'
commit_hash='d26d00845e95dc7d781459d0c1a7fd48ea4b6be3'
archive_hash='f940d71e0a4e15c040776979c6c99cb3520208744b3c22921f484d70ba82d675'
files=(
    "https://github.com/shamazmazum/quake2sdl/archive/${commit_hash}.tar.gz#${archive_hash}"
)
workdir="quake2sdl-${commit_hash}"
makeopts=()
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
depends=('SDL2')
launcher_name='Quake II'
launcher_category='&Games'
launcher_command='/usr/local/bin/quake2'
icon_file='docs/quake2.gif'

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
