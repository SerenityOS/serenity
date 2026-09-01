#!/usr/bin/env -S bash ../.port_include.sh
port='x265'
version='4.3'
workdir="${port}_${version}"
files=(
    "https://github.com/Multicorewareinc/x265/releases/download/${version}/x265_${version}.tar.gz#83c53e4c8bbb8f1e33ed59e10a7d621d1d7801ca853910c3eb41f038b8ffb121"
)
useconfigure='true'

configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)

configure() {
    run cmake "${configopts[@]}" source
}

install() {
    run make install
}
