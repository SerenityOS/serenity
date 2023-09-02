#!/usr/bin/env -S bash ../.port_include.sh
port=figlet
version=2.2.5
files=(
    "http://ftp.figlet.org/pub/figlet/program/unix/figlet-${version}.tar.gz#bf88c40fd0f077dab2712f54f8d39ac952e4e9f2e1882f1195be9e5e4257417d"
)

build() {
    run make CC="${CC}" LD="${CC}" "${makeopts[@]}"
}
