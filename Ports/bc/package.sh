#!/usr/bin/env -S bash ../.port_include.sh
port='bc'
version='7.1.0'
files=(
    "https://github.com/gavinhoward/bc/releases/download/${version}/bc-${version}.tar.xz#1f13663ba0f2435b684321714a4d0b9fff32bb951fc78dc7424cd69bba5c0d3a"
)
useconfigure='true'
configscript='configure.sh'
configopts=("--prefix=/usr/local" "--disable-nls")

export CFLAGS='-O3 -flto -std=c17'

configure() {
    run ./"${configscript}" "${configopts[@]}"
}
