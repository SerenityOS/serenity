#!/usr/bin/env -S bash ../.port_include.sh
port='bdwgc'
version='8.2.8'
use_fresh_config_sub='true'
files=(
    "https://github.com/ivmai/bdwgc/releases/download/v${version}/gc-${version}.tar.gz#7649020621cb26325e1fb5c8742590d92fb48ce5c259b502faf7d9fb5dabb160"
)
depends=(
    'libatomic_ops'
)
workdir="gc-${version}"
useconfigure='true'
configopts=(
    '--enable-threads=posix'
)

pre_configure() {
    run ./autogen.sh
}
