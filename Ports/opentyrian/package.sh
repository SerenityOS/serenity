#!/usr/bin/env -S bash ../.port_include.sh
port='opentyrian'
version='9750f8cfab738d0ea08ccb8d8752b95f5c09df07'
useconfigure='true'
files=(
    "https://github.com/opentyrian/opentyrian/archive/${version}.tar.gz#f9cd08210df3990c0bc3ac9241694bd6c58e0ddec4716b6e74a7cc655637e5a0"
    "http://camanis.net/tyrian/tyrian21.zip#7790d09a2a3addcd33c66ef063d5900eb81cc9c342f4807eb8356364dd1d9277"
)
configopts=(
    '-DCMAKE_BUILD_TYPE=release'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
depends=(
    'SDL2'
    'SDL2_net'
)

launcher_name='OpenTyrian'
launcher_category='&Games'
launcher_command='/usr/local/bin/opentyrian'
icon_file='linux/icons/tyrian-128.png'

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make install
    run_nocd rm -f tyrian21/*.exe
    run_nocd mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/share/games/opentyrian/"
    run_nocd cp -a tyrian21/* "${SERENITY_INSTALL_ROOT}/usr/local/share/games/opentyrian/"
}
