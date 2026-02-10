#!/usr/bin/env -S bash ../.port_include.sh
port='bdwgc'
version='8.2.12'
files=(
    "https://github.com/bdwgc/bdwgc/releases/download/v${version}/gc-${version}.tar.gz#42e5194ad06ab6ffb806c83eb99c03462b495d979cda782f3c72c08af833cd4e"
)
workdir="gc-${version}"
useconfigure='true'
configopts=(
    '--enable-threads=posix'
    # libatomic_ops is not needed when using a modern compiler, but the automatic detection does not work when cross-compiling.
    '--with-libatomic_ops=none'
)
