#!/usr/bin/env -S bash ../.port_include.sh
port=libuv
version=b12699b1efabfd241324f4ab6cfd6ce576db491e
useconfigure=true
files="https://github.com/libuv/libuv/archive/$version.tar.gz $port-$version.tar.gz bbbfa2bb50437047efc8fb29c243c914ae0de94107d7cc641c2f84e292904eb5"
auth_type=sha256
configopts="-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt -GNinja"

configure() {
    run cmake $configopts .
}

build() {
    run ninja
}

install() {
    run ninja install
}
