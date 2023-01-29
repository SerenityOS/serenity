#!/usr/bin/env -S bash ../.port_include.sh
port='dtc'
version='98a07006c48dc0bc3f42b3b3ce75b7f03e87e724'
auth_type='sha256'
files="https://github.com/dgibson/dtc/archive/${version}.tar.gz dtc-${version}.tar.gz 34a06bc0b3d0a3f411d09941946e0d094e7be81e437bdf6a7e30fa9c10de4bf4"
depends=('bash')


build() {
    run make NO_PYTHON=1
}

install() {
    run make PREFIX="${DESTDIR}" BINDIR="${DESTDIR}/usr/bin" install
}
