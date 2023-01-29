#!/usr/bin/env -S bash ../.port_include.sh
port=stpuzzles
useconfigure=true
version=git
workdir="${port}-main"
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files="https://github.com/SerenityPorts/stpuzzles/archive/refs/heads/main.zip ${port}.zip"

configure() {
    run cmake "${configopts[@]}" -DCMAKE_CXX_FLAGS="-std=c++2a -O2"
}

install() {
    run make install

    for puzzle in bridges cube dominosa fifteen filling flip flood galaxies guess inertia keen lightup loopy magnets map mines mosaic net netslide palisade pattern pearl pegs range rect samegame signpost singles sixteen slant solo tents towers tracks twiddle undead unequal unruly untangle; do
        install_launcher "$puzzle" "Games/Puzzles" "/usr/local/bin/$puzzle"
        install_icon "static-icons/${puzzle}.ico" "/usr/local/bin/$puzzle"
    done
}
