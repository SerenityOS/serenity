#!/usr/bin/env -S bash ../.port_include.sh
port='libatomic_ops'
version='7.8.2'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://github.com/ivmai/libatomic_ops/releases/download/v${version}/libatomic_ops-${version}.tar.gz#d305207fe207f2b3fb5cb4c019da12b44ce3fcbc593dfd5080d867b1a2419b51"
)
