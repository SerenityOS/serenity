#!/usr/bin/env -S bash ../.port_include.sh
port='SRB2'
useconfigure='true'
version='2.2.15'
short_version=${version//./}
depends=(
    'curl'
    'glu'
    'libpng'
    'SDL2'
    'SDL2_mixer'
)
configopts=(
    '-B build'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    # We can't use PORT_BUILD_DIR here as it's not defined yet
    "-DSRB2_CONFIG_ASSET_DIRECTORY=${SERENITY_BUILD_DIR}/Ports/SRB2"
    '-DCMAKE_C_STANDARD=17'
    '-DSRB2_SDL2_EXE_NAME=srb2'
    '-DSRB2_CONFIG_EXECINFO=OFF'
)
files=(
    "https://github.com/STJr/SRB2/archive/refs/tags/SRB2_release_${version}.tar.gz#c2689074539bf23d81e8add5f2beeac1b69d5586d69d93d44f6f6b195076bbaf"
    "https://github.com/STJr/SRB2/releases/download/SRB2_release_${version}/SRB2-v${short_version}-Full.zip#3eda3080ab87940fca5e4dcd22b8e041dc1d9e65bfe9177bff68383e9bd58a10"
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
    run cmake --build build
}

install() {
    # This file is being installed despite not being the release archive,
    # the source only seems to have referenced it in 2.2.12 and 2.2.13.
    # https://github.com/STJr/SRB2/blob/639b58c6d718452ef343a0bc927d043bed9e40d6/assets/CMakeLists.txt#L32
    # https://github.com/STJr/SRB2/blob/639b58c6d718452ef343a0bc927d043bed9e40d6/src/doomdef.h#L169-L171
    run_nocd touch "${PORT_BUILD_DIR}/patch.pk3"
    run cmake --install build --prefix "${SERENITY_INSTALL_ROOT}${install_dir}"
}
