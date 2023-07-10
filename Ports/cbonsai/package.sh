#!/usr/bin/env -S bash ../.port_include.sh
port='cbonsai'
version='1.3.1'
files=(
    "https://gitlab.com/jallbrit/cbonsai/-/archive/v${version}/cbonsai-v${version}.tar cbonsai-v${version}.tar.bz2 686e4af58f8e44e09b689da6e9a98a404fc0a779da7c7da1a5403283e0e471d8"
)
workdir="cbonsai-v${version}"
makeopts+=(CC="${CC}")
depends=("ncurses")

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
    run cp cbonsai "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
}
