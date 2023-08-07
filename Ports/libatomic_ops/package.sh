#!/usr/bin/env -S bash ../.port_include.sh

port=libatomic_ops
version=7.6.12
useconfigure=true
use_fresh_config_sub=true
files=(
    "https://www.hboehm.info/gc/gc_source/libatomic_ops-${version}.tar.gz f0ab566e25fce08b560e1feab6a3db01db4a38e5bc687804334ef3920c549f3e"
)
