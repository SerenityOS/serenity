#!/usr/bin/env -S bash ../.port_include.sh
port=cmatrix
useconfigure=true
version=3112b127babe72d2222059edd2d7eb7fb8bddfb1
depends=("ncurses")
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files=(
    "https://github.com/abishekvashok/cmatrix/archive/${version}.tar.gz a1d313d49a39cb5ae3a1c675872712f9f871114a161c38cbe94ce78967825f87"
)
launcher_name=cmatrix
launcher_category=Games
launcher_command='/usr/local/bin/cmatrix'
launcher_run_in_terminal=true

configure() {
    run cmake "${configopts[@]}"
}

install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp cmatrix "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
