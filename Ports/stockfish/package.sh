#!/usr/bin/env -S bash ../.port_include.sh
port='stockfish'
version='15.1'
useconfigure='false'
files="https://github.com/official-stockfish/Stockfish/archive/refs/tags/sf_${version}.tar.gz sf_${version}.tar.gz d4272657905319328294355973faee40a8c28e3eecb0e7b266ed34ff33383b76"
auth_type='sha256'
workdir="Stockfish-sf_${version}/src/"
makeopts+=(ARCH="${SERENITY_ARCH}" SUPPORTED_ARCH=true COMPCXX="${CXX}")

build() {
    run make build "${makeopts[@]}" 
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
    run cp stockfish "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
}
