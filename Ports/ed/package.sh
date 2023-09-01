#!/usr/bin/env -S bash ../.port_include.sh
port='ed'
version='1.19'
files=(
    "https://ftpmirror.gnu.org/gnu/ed/ed-${version}.tar.lz#ce2f2e5c424790aa96d09dacb93d9bbfdc0b7eb6249c9cb7538452e8ec77cd48"
)
useconfigure='true'
depends=(
    'pcre2'
)

configure() {
    run ./"${configscript}"
}
