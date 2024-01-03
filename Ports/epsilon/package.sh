#!/usr/bin/env -S bash ../.port_include.sh
port=epsilon
version=15.5.0
files=(
    "https://github.com/numworks/epsilon/archive/refs/tags/${version}.tar.gz#38c3b6baaf00863bbd179bce5e9cc42bbdbd0cd485b5bf3bbf4473383591bf83"
)
makeopts=("PLATFORM=simulator" "TARGET=serenity" "SERENITY_INSTALL_ROOT=${SERENITY_INSTALL_ROOT}")
depends=("SDL2" "libpng" "libjpeg" "freetype")
launcher_name=Epsilon
launcher_category='&Utilities'
launcher_command=/usr/local/bin/epsilon.elf

install() {
    run cp output/release/simulator/serenity/epsilon.elf ${SERENITY_INSTALL_ROOT}/usr/local/bin/
}
