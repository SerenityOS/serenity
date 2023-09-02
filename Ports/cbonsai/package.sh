#!/usr/bin/env -S bash ../.port_include.sh
port='cbonsai'
version='1.3.1'
files=(
    "https://gitlab.com/jallbrit/cbonsai/-/archive/v${version}/cbonsai-v${version}.tar.bz2#38531a5f5584185d63b7bcd4a308cad9f61cd829b676c221d254bdcb39c67427"
)
workdir="cbonsai-v${version}"
makeopts+=(CC="${CC}")
depends=("ncurses")

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
    run cp cbonsai "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
}
