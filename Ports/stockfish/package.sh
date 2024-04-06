#!/usr/bin/env -S bash ../.port_include.sh
port='stockfish'
version='16.1'
useconfigure='false'
files=(
    "https://github.com/official-stockfish/Stockfish/archive/refs/tags/sf_${version}.tar.gz#a5f94793b5d4155310397ba89e9c4266570ef0f24cd47de41a9103556f811b82"
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
