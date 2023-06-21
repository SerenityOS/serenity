#!/usr/bin/env -S bash ../.port_include.sh
port=opentyrian
version=84b820f852f3f6b812b4d00d6b3906adbbf3bbdb
useconfigure=true
files="https://github.com/opentyrian/opentyrian/archive/${version}.tar.gz ${version}.tar.gz 7429cc8e3468e3462b886cb99fe6cc0f5d232c193b68a94dc427493107c30dec"
auth_type=sha256
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
depends=("SDL2" "opentyrian-data")

launcher_name=OpenTyrian
launcher_category=Games
launcher_command=/usr/local/bin/tyrian

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
