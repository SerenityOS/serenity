#!/usr/bin/env -S bash ../.port_include.sh
port='stockfish'
version='19'
useconfigure='false'
files=(
    "https://github.com/official-stockfish/Stockfish/archive/refs/tags/sf_${version}.tar.gz#519b653d0d1ffb96531d982ccbe5c6a19425e8388e0e3c2f70f34b424ab32d76"
)
workdir="Stockfish-sf_${version}/src/"
makeopts+=(ARCH="${SERENITY_ARCH}" SUPPORTED_ARCH=true COMPCXX="${CXX}")

build() {
    run make build "${makeopts[@]}" 
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
    run cp stockfish "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
}
