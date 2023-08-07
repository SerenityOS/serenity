#!/usr/bin/env -S bash ../.port_include.sh
port=hatari
useconfigure=true
version=2.4.0-devel
depends=("SDL2" "zlib")
commit=6a86f054cc560a858bbe60c7529dafe2cf6ec604
workdir="${port}-${commit}"
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files=(
    "https://github.com/hatari/hatari/archive/${commit}.tar.gz bcb5d2e3bf3a3f8b34d21565354aa8eb085e3d92eb466c5d28e42e2022e7da3d"
)
launcher_name=Hatari
launcher_category=Games
launcher_command=hatari

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
