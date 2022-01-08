#!/usr/bin/env -S bash ../.port_include.sh

port=libatomic_ops
version=7.6.10
useconfigure=true
use_fresh_config_sub=true
files="https://www.hboehm.info/gc/gc_source/libatomic_ops-${version}.tar.gz libatomic_ops-${version}.tar.gz 587edf60817f56daf1e1ab38a4b3c729b8e846ff67b4f62a6157183708f099af"
auth_type=sha256
