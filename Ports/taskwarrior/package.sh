#!/usr/bin/env -S bash ../.port_include.sh
port='taskwarrior'
version='2.6.2'
useconfigure='true'
files="https://github.com/GothenburgBitFactory/taskwarrior/releases/download/v${version}/task-${version}.tar.gz taskwarrior-${version}.tar.gz b1d3a7f000cd0fd60640670064e0e001613c9e1cb2242b9b3a9066c78862cfec"
auth_type='sha256'
workdir="task-${version}"
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DENABLE_SYNC=OFF" "-DTASK_DOCDIR=share/doc/taskwarrior-2.6.2" "-DTASK_RCDIR=share/taskwarrior/rc")
depends=("libuuid")

configure() {
    mkdir -p taskwarrior-build
    cmake -G "Unix Makefiles" \
    -S task-${version} \
    -B taskwarrior-build \
    "${configopts[@]}"
}

build() {
    make -C taskwarrior-build
}

install() {
    make -C taskwarrior-build install
}
