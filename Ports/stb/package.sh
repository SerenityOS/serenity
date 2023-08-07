#!/usr/bin/env -S bash ../.port_include.sh
port='stb'
version='af1a5bc352164740c1cc1354942b1c6b72eacb8a'
files=(
    "https://github.com/nothings/stb/archive/${version}.zip e3d0edbecd356506d3d69b87419de2f9d180a98099134c6343177885f6c2cbef"
)

build() {
    :
}

install() {
    run cp -r "./" "${SERENITY_INSTALL_ROOT}/usr/local/include/"
}
