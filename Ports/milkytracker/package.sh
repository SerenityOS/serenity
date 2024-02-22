#!/bin/bash ../.port_include.sh
port=milkytracker
version=1.03.00
workdir=MilkyTracker-$version
useconfigure=true
files=(
    "https://github.com/milkytracker/MilkyTracker/archive/v$version.tar.gz#72d5357e303380b52383b66b51f944a77cd77e2b3bfeb227d87cc0e72ab292f7"
)
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
depends=("SDL2" "zlib")
launcher_name="MilkyTracker"
launcher_category='&Media'
launcher_command=/usr/local/bin/milkytracker

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
