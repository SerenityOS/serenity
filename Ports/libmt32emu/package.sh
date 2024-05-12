#!/usr/bin/env -S bash ../.port_include.sh
port='libmt32emu'
version='2.7.1'
workdir="munt-${port}_${version//./_}/mt32emu"
useconfigure='true'
configopts=(
    '-G Ninja'
    '-S .'
    '-DCMAKE_BUILD_TYPE:STRING=Release'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
files=(
    "https://github.com/munt/munt/archive/refs/tags/libmt32emu_${version//./_}.tar.gz#e4524d52d6799a4e32a961a2e92074f14adcb2f110a4e7a06bede77050cfdaf4"
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
