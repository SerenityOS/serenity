#!/usr/bin/env -S bash ../.port_include.sh
port=sam
version=c86ea395743b8ea4ad071c2167fd1f7f96648f7b
workdir="SAM-${version}"
files=(
    "https://github.com/vidarh/SAM/archive/${version}.tar.gz#1f534245e2c7a096de5f886fd96ea1ad966c4e674c1ed91e0c6a59662e8d6c11"
)
depends=("SDL2")

build() {
    run make CC="${CC}"
}

install() {
    run cp sam "${SERENITY_INSTALL_ROOT}/usr/local/bin/sam"
}
