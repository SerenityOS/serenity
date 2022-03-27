#!/usr/bin/env -S bash ../.port_include.sh
port=fheroes2
useconfigure=true
version=0.9.13
depends=("SDL2" "SDL2_image" "SDL2_mixer" "libpng" "zlib")
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DUSE_SDL_VERSION=SDL2" "-DENABLE_IMAGE=ON" "-DGET_HOMM2_DEMO=ON")
files="https://github.com/ihhub/fheroes2/archive/refs/tags/${version}.zip fheroes2-${version}.zip 879805bc88c3561d0eedc3dda425e8d9a3c7ae8a80b9f6909797acc72598cc17"
auth_type=sha256
launcher_name="Free Heroes of Might and Magic II"
launcher_category=Games
launcher_command=/opt/fheroes2/fheroes2
icon_file=src/resources/fheroes2.ico

pre_configure() {
    export CXXFLAGS="'-D_GNU_SOURCE'"
}

configure() {
    run cmake "${configopts[@]}" .
}

post_configure() {
    unset CXXFLAGS
}

install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/opt/fheroes2/files"
    run cp -r data/ maps/ fheroes2 fheroes2.key "${SERENITY_INSTALL_ROOT}/opt/fheroes2"
    run cp -r files/data "${SERENITY_INSTALL_ROOT}/opt/fheroes2/files"
}
