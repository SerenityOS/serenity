#!/usr/bin/env -S bash ../.port_include.sh
port='flare-game'
useconfigure='true'
version='1.15'
depends=(
    'flare-engine'
)
files=(
    "https://github.com/flareteam/flare-game/archive/refs/tags/v${version}.tar.gz#b6b2e7037dae416c29d2d55f8275491ed9edec690194fc2165a5429582a2518c"
)

configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
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
