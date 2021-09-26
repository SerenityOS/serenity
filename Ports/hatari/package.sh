#!/usr/bin/env -S bash ../.port_include.sh
port=hatari
useconfigure=true
version=2.4.0-devel
depends=("SDL2" "zlib")
commit=353379e1f8a847cc0e284541d2b40fd49d175d22
workdir="${port}-${commit}"
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files="https://github.com/hatari/hatari/archive/${commit}.tar.gz ${commit}.tar.gz 617f95b30c4e590bb61ddcc1dafc22f4bf270377caa7aa5867f3f7413250b538"
auth_type=sha256
launcher_name=Hatari
launcher_category=Games
launcher_command=hatari

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
