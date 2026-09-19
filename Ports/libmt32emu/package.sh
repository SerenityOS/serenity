#!/usr/bin/env -S bash ../.port_include.sh
port='libmt32emu'
version='2.8.2'
workdir="munt-${port}_${version//./_}/mt32emu"
useconfigure='true'
configopts=(
    '-G Ninja'
    '-S .'
    '-DCMAKE_BUILD_TYPE:STRING=Release'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
files=(
    "https://github.com/munt/munt/archive/refs/tags/libmt32emu_${version//./_}.tar.gz#d4778cf89b054ba7ab410ffcb02ecf1629fa32b5b60838addec99eb93804fdcb"
)

configure() {
    run cmake \
        "${configopts[@]}" \
        -B mt32emu-build
}

build() {
    run ninja -C mt32emu-build
}

install() {
    run ninja -C mt32emu-build install
}
