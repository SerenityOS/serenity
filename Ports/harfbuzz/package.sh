#!/usr/bin/env -S bash ../.port_include.sh
port=harfbuzz
useconfigure=true
version=2.8.1
depends=("freetype" "libicu")
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DHB_HAVE_FREETYPE=ON" "-DHB_HAVE_ICU=ON")
files="https://github.com/harfbuzz/harfbuzz/releases/download/${version}/harfbuzz-${version}.tar.xz harfbuzz-${version}.tar.xz 4124f663ec4bf4e294d9cf230668370b4249a48ff34deaf0f06e8fc82d891300"
auth_type=sha256

configure() {
    run mkdir -p build
    run sh -c "cd build && cmake ${configopts[@]} .."
}

build() {
    run sh -c "cd build && make ${makeopts[@]}"
}

install() {
    run sh -c "cd build && make install"
}
