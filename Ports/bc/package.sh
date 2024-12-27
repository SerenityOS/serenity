#!/usr/bin/env -S bash ../.port_include.sh
port='bc'
version='7.0.3'
files=(
    "https://github.com/gavinhoward/bc/releases/download/${version}/bc-${version}.tar.xz#91eb74caed0ee6655b669711a4f350c25579778694df248e28363318e03c7fc4"
)
useconfigure='true'
configscript='configure.sh'
configopts=("--prefix=/usr/local" "--disable-nls")

export CFLAGS='-O3 -flto'

configure() {
    run ./"${configscript}" "${configopts[@]}"
}
