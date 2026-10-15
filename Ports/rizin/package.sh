#!/usr/bin/env -S bash ../.port_include.sh
port='rizin'
version='0.9.1'
workdir="rizin-v${version}"
useconfigure='true'
files=(
    "https://github.com/rizinorg/rizin/releases/download/v${version}/rizin-src-v${version}.tar.xz#7ac1cd7daca7afdda742e15478b1f747fc1f813e496fee71839d1e109e543dca"
)

configopts=(
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    "--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    '--buildtype=release'
    '-Duse_sys_tree_sitter=enabled'
    '-Duse_sys_libzstd=enabled'
    '-Duse_sys_zlib=enabled'
    '-Duse_sys_lz4=enabled'
    '-Duse_sys_openssl=enabled'
    '-Duse_sys_libzip_openssl=true'
    '-Duse_sys_libzip=enabled'
    '-Duse_sys_lz4=enabled'
    '-Duse_sys_pcre2=enabled'
    '-Ddebugger=false'
)

depends=(
    'libzip'
    'lz4'
    'openssl'
    'pcre2'
    'tree-sitter'
    'zlib'
    'zstd'
)

configure() {
    run meson setup build "${configopts[@]}"
}

build() {
    run ninja -C build
}

install() {
    run ninja -C build install
}
