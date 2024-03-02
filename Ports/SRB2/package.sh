#!/usr/bin/env -S bash ../.port_include.sh
port='SRB2'
useconfigure=true
version='2.2.13'
short_version=${version//./}
depends=(
    'curl'
    'glu'
    'libpng'
    'SDL2'
    'SDL2_mixer'
)
configopts=(
    '-B./build'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DSRB2_CONFIG_ENABLE_TESTS=OFF'
    '-DSRB2_CONFIG_SYSTEM_LIBRARIES=ON'
    '-DSRB2_SDL2_EXE_NAME=srb2'
)
files=(
    "https://github.com/STJr/SRB2/archive/refs/tags/SRB2_release_${version}.tar.gz#0fc460dc93c056c21bfcc389ac0515588673ee692968d9a6711b19e63d283b3f"
    "https://github.com/STJr/SRB2/releases/download/SRB2_release_${version}/SRB2-v${short_version}-Full.zip#83b91a351135b63705e49daffa44e7ac3cf3e33b397f56ff347ebb71eda27d4a"
)
workdir="SRB2-SRB2_release_${version}"
launcher_name='Sonic Robo Blast 2'
launcher_category='&Games'
launcher_command='/usr/local/games/SRB2/srb2'
icon_file='srb2.png'

install_dir='/usr/local/games/SRB2'

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run make -C build -j${MAKEJOBS}
}

install() {
    run /usr/bin/install -d "${SERENITY_INSTALL_ROOT}${install_dir}/"
    run /usr/bin/install ./build/bin/srb2 ./srb2.png "${SERENITY_INSTALL_ROOT}${install_dir}"
    run cp -r ${PWD}/*.{dat,dta,pk3,txt} "${SERENITY_INSTALL_ROOT}${install_dir}/"
    run cp -r "../models" "${SERENITY_INSTALL_ROOT}${install_dir}/"
}
