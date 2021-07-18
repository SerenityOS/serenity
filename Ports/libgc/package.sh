#!/usr/bin/env -S bash ../.port_include.sh

port=libgc
version=8.0.4
workdir="gc-${version}"
useconfigure=true
files="https://www.hboehm.info/gc/gc_source/gc-${version}.tar.gz gc-${version}.tar.gz 436a0ddc67b1ac0b0405b61a9675bca9e075c8156f4debd1d06f3a56c7cd289d"
auth_type=sha256
depends="libatomic_ops"
