#!/usr/bin/env -S bash ../.port_include.sh
port=acpica-tools
version=20220331
workdir="acpica-unix-${version}"
files="https://acpica.org/sites/acpica/files/acpica-unix-${version}.tar.gz acpica-unix-${version}.tar.gz acaff68b14f1e0804ebbfc4b97268a4ccbefcfa053b02ed9924f2b14d8a98e21"


build() {
    run make iasl
    run make acpixtract
    # FIXME: Make "run make acpiexec" to work
    run make acpihelp
    run make acpisrc
    run make acpibin
}
