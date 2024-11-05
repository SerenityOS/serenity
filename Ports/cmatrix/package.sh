#!/usr/bin/env -S bash ../.port_include.sh
port='cmatrix'
useconfigure='true'
version='5c082c64a1296859a11bee60c8c086655953a416'
depends=(
    'ncurses'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
files=(
    "https://github.com/abishekvashok/cmatrix/archive/${version}.tar.gz#722fb8e8d4879adf3185f720d5a927134e64c9c7346f603910d235385359f8c3"
)
launcher_name='cmatrix'
launcher_category='&Games'
launcher_command='/usr/local/bin/cmatrix'
launcher_run_in_terminal='true'

configure() {
    run cmake "${configopts[@]}"
}

install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp cmatrix "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
