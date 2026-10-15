#!/usr/bin/env -S bash ../.port_include.sh
port='milkytracker'
version='1.06'
workdir="MilkyTracker-${version}"
useconfigure='true'
files=(
    "https://github.com/milkytracker/MilkyTracker/archive/v$version.tar.gz#6e70590dfed324e6d6ac813e33d9f9dcfaa13b2f57fdec9e178e9dda05538cb0"
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
