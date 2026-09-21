#!/usr/bin/env -S bash ../.port_include.sh
port='flare-engine'
useconfigure='true'
version='1.15'
depends=(
    'SDL2'
    'SDL2_image'
    'SDL2_mixer'
    'SDL2_ttf'
)
files=(
    "https://github.com/flareteam/flare-engine/archive/refs/tags/v${version}.tar.gz#642db16111487bf8a3fb7ebadcfc635909c7eaf91df7723ee22fae1caac9fe93"
)

configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DSDL2_INCLUDE_DIR=${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
    # Toolchain sets CMAKE_INSTALL_PREFIX to /usr/local, which flare uses as a
    # path prefix for install destinations. CMake only prepends DESTDIR to
    # relative paths, so use a relative prefix to keep installs under DESTDIR.
    "-DCMAKE_INSTALL_PREFIX=usr/local"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make "${installopts[@]}" install
}

launcher_name='Flare'
launcher_category='&Games'
launcher_command='/usr/local/games/flare'
icon_file='distribution/flare_logo_icon.png'
