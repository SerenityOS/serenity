#!/usr/bin/env -S bash ../.port_include.sh
port=dungeonrush
version=1.1-beta
useconfigure=true
workdir="DungeonRush-${version}"
files=(
    "https://github.com/Rapiz1/DungeonRush/archive/refs/tags/v${version}.tar.gz#295b83cb023bf5d21318992daee125399892bdf16a87c835dfc90b841c929eda"
)
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
depends=("SDL2" "SDL2_image" "SDL2_mixer" "SDL2_ttf" "SDL2_net")
launcher_name="DungeonRush"
launcher_category='&Games'
launcher_command=/opt/DungeonRush/dungeon_rush
icon_file=dungeonrush.png

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/opt/DungeonRush"
    run cp -r bin/dungeon_rush res "${SERENITY_INSTALL_ROOT}/opt/DungeonRush"
}
