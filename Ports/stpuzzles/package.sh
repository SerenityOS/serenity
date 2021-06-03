#!/usr/bin/env -S bash ../.port_include.sh
port=SimonTathamsPuzzles
useconfigure=true
version=git
workdir=stpuzzles-main
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_SOURCE_DIR/Toolchain/CMake/CMakeToolchain.txt"
files="https://github.com/SerenityOS/stpuzzles/archive/refs/heads/main.zip stpuzzles.zip 425addbfa4949f6f7341f476359789a33f3826c50f3dc2f6aa734c423a8926e7"
auth_type=sha256

configure() {
    run cmake $configopts -DCMAKE_CXX_FLAGS="-std=c++2a -O2"
}

install() {
    run make install

    for puzzle in bridges cube dominosa fifteen filling flip flood galaxies guess inertia keen lightup loopy magnets map mines mosaic net netslide palisade pattern pearl pegs range rect samegame signpost singles sixteen slant solo tents towers tracks twiddle undead unequal unruly untangle; do
        install_launcher "$puzzle" "Games/Puzzles" "/usr/local/bin/$puzzle"
    done
}
