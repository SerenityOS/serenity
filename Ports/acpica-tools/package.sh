#!/usr/bin/env -S bash ../.port_include.sh
port='acpica-tools'
version='20260930'
workdir="acpica-${version}"
files=(
    "https://github.com/open-acpica/acpica/archive/refs/tags/${version}.tar.gz#feddab0f42f1e01afa3b8ec04f3aaef5e61e891225b8dc8776953ca9f3df1449"
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
