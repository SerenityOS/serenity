#!/usr/bin/env -S bash ../.port_include.sh
port='aria2'
version='1.36.0'
files=(
    "https://github.com/aria2/aria2/releases/download/release-${version}/aria2-${version}.tar.xz 58d1e7608c12404f0229a3d9a4953d0d00c18040504498b483305bcb3de907a5"
)
depends=(
    'libssh2'
    'libuv'
    'libxml2'
    'openssl'
    'zlib'
)
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths+=(
    'deps/wslay/config.sub'
)
configopts+=(
    '--with-libuv'
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
    '--without-libcares'
    '--without-sqlite3'
)
