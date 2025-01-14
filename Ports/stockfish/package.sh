#!/usr/bin/env -S bash ../.port_include.sh
port='stockfish'
version='17'
useconfigure='false'
files=(
    "https://github.com/official-stockfish/Stockfish/archive/refs/tags/sf_${version}.tar.gz#8f9b52285c3348c065b7cb58410626df16d7416a2e60a3b04f3ec7c038e67ad1"
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
