#!/usr/bin/env -S bash ../.port_include.sh
port='bdwgc'
version='8.2.4'
use_fresh_config_sub='true'
files=(
    "https://github.com/ivmai/bdwgc/releases/download/v${version}/gc-${version}.tar.gz#3d0d3cdbe077403d3106bb40f0cbb563413d6efdbb2a7e1cd6886595dec48fc2"
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
