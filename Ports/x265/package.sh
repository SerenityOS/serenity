#!/usr/bin/env -S bash ../.port_include.sh
port=x265
version=3.5
workdir="${port}_${version}"
files=(
    "https://bitbucket.org/multicoreware/x265_git/downloads/x265_${version}.tar.gz x265_${version}.tar.gz e70a3335cacacbba0b3a20ec6fecd6783932288ebc8163ad74bcc9606477cae8"
)
useconfigure=true

configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)

configure() {
    run cmake "${configopts[@]}" source
}

install() {
    run make install
}
