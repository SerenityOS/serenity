#!/usr/bin/env -S bash ../.port_include.sh
port='SRB2'
useconfigure=true
version='2.2.13'
depends=(
    'curl'
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
    "https://git.do.srb2.org/STJr/srb2assets-public/-/archive/SRB2_release_${version}/srb2assets-public-SRB2_release_${version}.tar.gz#edcc98a7ad0c0878013b39ca3941cdf8094f6cbab5129911abe3022170e96b8a"
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
    run cp -r "../srb2assets-public-SRB2_release_${version}/." "${SERENITY_INSTALL_ROOT}${install_dir}/"
}
