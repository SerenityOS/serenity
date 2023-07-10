#!/usr/bin/env -S bash ../.port_include.sh
port='stockfish'
version='16'
useconfigure='false'
files=(
    "https://github.com/official-stockfish/Stockfish/archive/refs/tags/sf_${version}.tar.gz sf_${version}.tar.gz a1600ebdaf4e324ba3e10cec2e0c9a810dc64c6f0db5cc955b2fd5e1eefa1cc6"
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
