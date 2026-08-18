#!/usr/bin/env -S bash ../.port_include.sh
port='chester'
version='2581716abb6a538535d07c71802f9571ba08e1bc'
depends=(
    'SDL2'
)
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    # Upstream declares 3.0.0 which is no longer supported
    '-DCMAKE_POLICY_VERSION_MINIMUM=3.25'
)
files=(
    "https://github.com/veikkos/chester/archive/${version}.zip#024e58247f1a95fde905230ef2ca8e0f953452f0045448cb97f5591baacf5408"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run cp bin/chester "${SERENITY_INSTALL_ROOT}/bin"
}
