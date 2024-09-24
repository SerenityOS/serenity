#!/usr/bin/env -S bash ../.port_include.sh
port='x265'
version='3.6'
workdir="${port}_${version}"
files=(
    "https://bitbucket.org/multicoreware/x265_git/downloads/x265_${version}.tar.gz#663531f341c5389f460d730e62e10a4fcca3428ca2ca109693867bc5fe2e2807"
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
