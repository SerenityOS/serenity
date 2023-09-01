#!/usr/bin/env -S bash ../.port_include.sh
port='libatomic_ops'
version='7.6.14'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://www.hboehm.info/gc/gc_source/libatomic_ops-${version}.tar.gz#390f244d424714735b7050d056567615b3b8f29008a663c262fb548f1802d292"
)
