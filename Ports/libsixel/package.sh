#!/usr/bin/env -S bash ../.port_include.sh
port=libsixel
version=1.8.6
files="https://github.com/saitoha/libsixel/archive/refs/tags/v${version}.tar.gz ${port}-${version}.tar.gz 37611d60c7dbcee701346967336dbf135fdd5041024d5f650d52fae14c731ab9"
useconfigure=true
auth_type=sha256
configopts=("--prefix=${SERENITY_INSTALL_ROOT}/usr/local")

install() {
    run make install
}
