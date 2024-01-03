#!/usr/bin/env -S bash ../.port_include.sh
port='serious-sam-classic'
useconfigure='false'
version='1.10.4'
files=(
    "https://github.com/tx00100xt/SeriousSamClassic/archive/refs/tags/v${version}.tar.gz#c42e1434e03f713ffc60aa627f0a24c64287598bc5ee7cdbd2cbe91aa363ef51"
)
depends=(
    'libvorbis'
    'SDL2'
    'zlib'
)
workdir="SeriousSamClassic-${version}"

launcher_name='Serious Sam - The First Encounter'
launcher_category='&Games'
launcher_command="/usr/local/bin/serioussam"
icon_file='SamTFE/Sources/SeriousSam/res/SeriousSam.ico'

sam_tfe_dir="${workdir}/SamTFE"
sam_tfe_build_dir="${sam_tfe_dir}/Sources/cmake-build"

# FIXME: SeriousSamClassic includes both TFE and TSE; we should also build and install TSE

build() {
    # Host build: ecc
    mkdir -p "${sam_tfe_build_dir}"
    cd "${sam_tfe_build_dir}"
    if [ ! -x 'ecc' ]; then
        host_env
        cmake \
            -DTFE=true \
            -DUSE_SYSTEM_SDL2=0 \
            ../
        make "${makeopts[@]}" ecc
    fi

    # Target build: SamTFE
    cp -vfr ../Entities/PlayerWeapons_old.es ../Entities/PlayerWeapons.es
    target_env
    cmake \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt \
        -DECC=${PORT_BUILD_DIR}/${sam_tfe_build_dir}/ecc \
        -DINTERNAL_NAME=serioussam \
        -DLINUX=1 \
        -DTFE=true \
        -DUSE_SYSTEM_SDL2=1 \
        ../
    make "${makeopts[@]}"
}

install() {
    cd "${sam_tfe_build_dir}"
    make "${installopts[@]}" install
}

post_install() {
    echo
    echo 'Serious Sam: The First Encounter is installed!'
    echo
    echo 'Make sure your game files are present in the following directory:'
    echo '    Inside SerenityOS: ~/.local/share/Serious-Engine/serioussam/'
    echo "    Outside SerenityOS: ${SERENITY_SOURCE_DIR}/Base/home/anon/.local/share/Serious-Engine/serioussam/"
    echo
    echo 'Copy over all files. The game will be looking for a directory with the `1_00_music.gro` file.'
}
