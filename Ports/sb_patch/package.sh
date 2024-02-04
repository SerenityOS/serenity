#!/usr/bin/env -S bash ../.port_include.sh
port='sb_patch'
useconfigure='true'
version='6059f85d23c9715b1193367d0f4bd140220c859d'
files=(
    "git+https://github.com/shannonbooth/patch.git#${version}"
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)

configure() {
    run cmake . "${configopts[@]}"
}

build() {
    run cmake --build .
}

install() {
    run cmake --build . --target install
}
