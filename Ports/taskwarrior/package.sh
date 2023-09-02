#!/usr/bin/env -S bash ../.port_include.sh
port='taskwarrior'
version='2.6.2'
useconfigure='true'
files=(
    "https://github.com/GothenburgBitFactory/taskwarrior/releases/download/v${version}/task-${version}.tar.gz#b1d3a7f000cd0fd60640670064e0e001613c9e1cb2242b9b3a9066c78862cfec"
)
workdir="task-${version}"
configopts=("-DCMAKE_BUILD_TYPE=release" "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DENABLE_SYNC=OFF" "-DTASK_DOCDIR=share/doc/taskwarrior-2.6.2" "-DTASK_RCDIR=share/taskwarrior/rc")
depends=("libuuid")

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make install
}
