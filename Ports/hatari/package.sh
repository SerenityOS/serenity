#!/usr/bin/env -S bash ../.port_include.sh
port='hatari'
useconfigure='true'
version='2.6.1'
depends=(
    'SDL2'
    'zlib'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
files=(
    "https://github.com/hatari/hatari/archive/refs/tags/v${version}.tar.gz#75f09c8deddb511b8c1574b84c507743097db9abc6edbb078f942efa8677547e"
)
launcher_name='Hatari'
launcher_category='&Games'
launcher_command='/usr/local/bin/hatari'
icon_file='share/icons/hicolor/32x32/apps/hatari.png'

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make DESTDIR="${SERENITY_INSTALL_ROOT}" install
}
