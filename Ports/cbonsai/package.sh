#!/usr/bin/env -S bash ../.port_include.sh
port='cbonsai'
version='1.4.2'
files=(
    "https://gitlab.com/jallbrit/cbonsai/-/archive/v${version}/cbonsai-v${version}.tar.bz2#1ded632e90644d29ad6c142c62aa2e66eab3c7a6dee23dfb6f7a12a973c3fc50"
)
workdir="cbonsai-v${version}"
makeopts+=(CC="${CC}")
depends=("ncurses")

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
    run cp cbonsai "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
}
