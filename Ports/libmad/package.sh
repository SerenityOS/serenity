#!/usr/bin/env -S bash ../.port_include.sh
port='libmad'
version='0.16.4'
workdir='libmad'
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    # Upstream declares 3.1 which is no longer supported
    "-DCMAKE_POLICY_VERSION_MINIMUM=3.25"
    "-DEXAMPLE=OFF"
)
files=(
    "https://codeberg.org/tenacityteam/libmad/releases/download/${version}/libmad-${version}.tar.gz#0f6bfb36c554075494b5fc2c646d08de7364819540f23bab30ae73fa1b5cfe65"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
