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
    mkdir -p "${workdir}/host-build" "${workdir}/build"

    # The 'icons' cmake target generates a .ico for each puzzle by building and
    # running the puzzle binaries on the host.
    host_env
    (
        cd "${workdir}/host-build"
        run_nocd cmake .. -DCMAKE_BUILD_TYPE=Release
    )

    target_env
    (
        cd "${workdir}/build"
        run_nocd cmake .. "${configopts[@]}"
    )
}

build() {
    host_env
    (
        cd "${workdir}/host-build"
        run_nocd cmake --build . --target icons "${makeopts[@]}"
    )

    if command -v magick >/dev/null; then
        magick_convert=magick
    elif command -v convert >/dev/null; then
        magick_convert=convert
    else
        echo 'Unable to convert icon: missing magick/convert or identify, did you install ImageMagick?'
        exit 1
    fi

    # ImageMagick decodes the generated .ico files as fully transparent.
    # Other software doesn't and I'm not sure who is at fault, but we can
    # simply rebuild each .ico from the opaque source PNGs.
    for icon in "${workdir}/host-build/icons/"*-16d24.png; do
        name=${icon%-16d24.png}
        run_nocd "${magick_convert}" "${name}-16d24.png" "${name}-32d24.png" "${name}.ico"
    done

    target_env
    (
        cd "${workdir}/build"
        run_nocd cmake --build . "${makeopts[@]}"
    )
}

install() {
    (
        cd "${workdir}/build"
        run_nocd cmake --install .
    )

    for puzzle in blackbox bridges cube dominosa fifteen filling flip flood galaxies guess inertia keen lightup loopy magnets map mines mosaic net netslide palisade pattern pearl pegs range rect samegame signpost singles sixteen slant solo tents towers tracks twiddle undead unequal unruly untangle; do
        install_launcher "$puzzle" "&Games/Puzzles" "/usr/local/bin/$puzzle" ""
        install_icon "host-build/icons/${puzzle}.ico" "/usr/local/bin/$puzzle"
    done
}
