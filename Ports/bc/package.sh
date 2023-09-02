#!/usr/bin/env -S bash ../.port_include.sh
port='bc'
version='6.5.0'
files=(
    "https://github.com/gavinhoward/bc/releases/download/${version}/bc-${version}.tar.xz#b1afb1f50c0bce6119c98590bcc8afc22f520bc85c2b512c83938dbb8321cc30"
)
useconfigure='true'
configscript='configure.sh'
configopts=("--prefix=/usr/local" "--disable-nls")

export CFLAGS='-O3 -flto'

configure() {
    run ./"${configscript}" "${configopts[@]}"
}
