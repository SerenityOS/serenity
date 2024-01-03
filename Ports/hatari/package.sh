#!/usr/bin/env -S bash ../.port_include.sh
port='hatari'
useconfigure='true'
version='2.4.1'
depends=(
    'SDL2'
    'zlib'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
files=(
    "https://github.com/hatari/hatari/archive/refs/tags/v${version}.tar.gz#68c5edbe60db7a83e6e9b427eaac1136b62653846d64e415850e88d9a6a2cbc2"
)
launcher_name='Hatari'
launcher_category='&Games'
launcher_command='/usr/local/bin/hatari'
icon_file='share/icons/hicolor/32x32/apps/hatari.png'

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
