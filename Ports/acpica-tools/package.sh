#!/usr/bin/env -S bash ../.port_include.sh
port=acpica-tools
version='20260408'
workdir="acpica-${version}"
files=(
    "https://github.com/open-acpica/acpica/archive/refs/tags/${version}.tar.gz#ddc5d3e0f54030e2348484fff681861a161efb4e388e20631209574e7884ad39"
)


build() {
    run make iasl
    run make acpixtract
    # FIXME: Make "run make acpiexec" to work
    run make acpihelp
    run make acpisrc
    run make acpibin
}

# acpica contains unknown warning flags (on Clang) and is generally littered with warnings,
# some of them intentional. Make sure that we at least don't error on them.
export NOWERROR='TRUE'
