#!/usr/bin/env -S bash ../.port_include.sh
port='x265'
version='4.1'
workdir="${port}_${version}"
files=(
    "https://bitbucket.org/multicoreware/x265_git/downloads/x265_${version}.tar.gz#a31699c6a89806b74b0151e5e6a7df65de4b49050482fe5ebf8a4379d7af8f29"
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
