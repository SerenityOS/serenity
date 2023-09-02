#!/usr/bin/env -S bash ../.port_include.sh
port=ed
version=1.18
files=(
    "https://ftpmirror.gnu.org/gnu/ed/ed-${version}.tar.lz#aca8efad9800c587724a20b97aa8fc47e6b5a47df81606feaba831b074462b4f"
)
useconfigure=true
depends=("pcre2")

configure() {
    run ./"$configscript"
}
