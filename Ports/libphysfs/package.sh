#!/usr/bin/env -S bash ../.port_include.sh
port=libphysfs
useconfigure=true
version=3.0.2
workdir="physfs-${version}"
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files=(
    "https://icculus.org/physfs/downloads/physfs-${version}.tar.bz2#304df76206d633df5360e738b138c94e82ccf086e50ba84f456d3f8432f9f863"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
