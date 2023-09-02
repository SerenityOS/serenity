#!/usr/bin/env -S bash ../.port_include.sh
port='dtc'
version='1.7.0'
files=(
    "https://github.com/dgibson/dtc/archive/refs/tags/v${version}.tar.gz#70d9c156ec86d63de0f7bdae50540ffa492b25ec1d69491c7520845c860b9a62"
)
depends=('bash')


build() {
    run make NO_PYTHON=1
}

install() {
    run make NO_PYTHON=1 PREFIX="${DESTDIR}" BINDIR="${DESTDIR}/usr/bin" install
}
