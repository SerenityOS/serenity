#!/usr/bin/env -S bash ../.port_include.sh
port='dtc'
version='1.8.1'
files=(
    "https://github.com/dgibson/dtc/archive/refs/tags/v${version}.tar.gz#74b50bb19134f6562490afea53e59953dd6c4afb17e5ccb60be32221262d3390"
)
depends=('bash')


build() {
    run make NO_PYTHON=1
}

install() {
    run make NO_PYTHON=1 PREFIX="${DESTDIR}" BINDIR="${DESTDIR}/usr/bin" install
}
