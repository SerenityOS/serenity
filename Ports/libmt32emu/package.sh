#!/usr/bin/env -S bash ../.port_include.sh
port='libmt32emu'
version='2.8.3'
workdir="munt-${port}_${version//./_}/mt32emu"
useconfigure='true'
configopts=(
    '-G Ninja'
    '-S .'
    '-DCMAKE_BUILD_TYPE:STRING=Release'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
files=(
    "https://github.com/munt/munt/archive/refs/tags/libmt32emu_${version//./_}.tar.gz#81f8c462f46bc8901618762ae34cf9de93894ff81f41db73c79472fa3baef875"
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
