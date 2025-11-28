#!/usr/bin/env -S bash ../.port_include.sh
port='libatomic_ops'
version='7.10.0'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://github.com/bdwgc/libatomic_ops/releases/download/v${version}/libatomic_ops-${version}.tar.gz#0db3ebff755db170f65e74a64ec4511812e9ee3185c232eeffeacd274190dfb0"
)
