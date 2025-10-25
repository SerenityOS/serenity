#!/usr/bin/env -S bash ../.port_include.sh
port='bdwgc'
version='8.2.10'
files=(
    "https://github.com/bdwgc/bdwgc/releases/download/v${version}/gc-${version}.tar.gz#832cf4f7cf676b59582ed3b1bbd90a8d0e0ddbc3b11cb3b2096c5177ce39cc47"
)
workdir="gc-${version}"
useconfigure='true'
configopts=(
    '--enable-threads=posix'
    # libatomic_ops is not needed when using a modern compiler, but the automatic detection does not work when cross-compiling.
    '--with-libatomic_ops=none'
)
