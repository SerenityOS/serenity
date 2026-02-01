#!/usr/bin/env -S bash ../.port_include.sh
port='rizin'
commit_hash='20769eaaa73660994b740a4ae0bdfa23a8d02d31'
version="0.9.0-dev.515+20769ea"
workdir="rizin-${commit_hash}"
useconfigure='true'
files=(
    "https://github.com/rizinorg/rizin/archive/${commit_hash}.tar.gz#69335598e215e2f3e8208b96941810076db34a93805b2827aa0dc5fcfe0f6d7d"
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
