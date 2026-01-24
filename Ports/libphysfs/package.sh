#!/usr/bin/env -S bash ../.port_include.sh
port=libphysfs
useconfigure=true
version=3.2.0
workdir="physfs-${version}"
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files=(
    "https://github.com/icculus/physfs/archive/refs/tags/release-${version}.tar.gz#1991500eaeb8d5325e3a8361847ff3bf8e03ec89252b7915e1f25b3f8ab5d560"
)
workdir="physfs-release-${version}"

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
