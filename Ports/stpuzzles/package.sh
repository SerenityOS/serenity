#!/usr/bin/env -S bash ../.port_include.sh
port='stpuzzles'
version='3c3632259d298ab62aafa8a5858823569ab1af46'
files=(
    "git+https://git.tartarus.org/simon/puzzles.git#${version}"
)
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_CXX_FLAGS=-O2"
)

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run cmake --build .
}

install() {
    run cmake --install .

    for puzzle in blackbox bridges cube dominosa fifteen filling flip flood galaxies guess inertia keen lightup loopy magnets map mines mosaic net netslide palisade pattern pearl pegs range rect samegame signpost singles sixteen slant solo tents towers tracks twiddle undead unequal unruly untangle; do
        install_launcher "$puzzle" "&Games/Puzzles" "/usr/local/bin/$puzzle" ""
        install_icon "${PORT_META_DIR}/static-icons/${puzzle}.ico" "/usr/local/bin/$puzzle"
    done
}
