#!/usr/bin/env -S bash ../.port_include.sh
port='stpuzzles'
version='8f3413c31ffd43c4ebde40894ac1b2f7cdf222c3'
files="https://git.tartarus.org/?p=simon/puzzles.git;a=snapshot;h=${version};sf=tgz puzzles-${version::7}.tar.gz 6af9bd93759d792e539131639dfae165398047262723827940da7209fc8260a7"
auth_type='sha256'
workdir="puzzles-${version::7}"
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_CXX_FLAGS=-std=c++2a -O2"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install

    for puzzle in bridges cube dominosa fifteen filling flip flood galaxies guess inertia keen lightup loopy magnets map mines mosaic net netslide palisade pattern pearl pegs range rect samegame signpost singles sixteen slant solo tents towers tracks twiddle undead unequal unruly untangle; do
        install_launcher "${puzzle}" "Games/Puzzles" "/usr/local/bin/${puzzle}"
        install_icon "static-icons/${puzzle}.ico" "/usr/local/bin/${puzzle}"
    done
}
