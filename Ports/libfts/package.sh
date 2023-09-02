#!/usr/bin/env -S bash ../.port_include.sh
port='libfts'
version='1.2.7'
files=(
    "https://github.com/void-linux/musl-fts/archive/refs/tags/v${version}.tar.gz#49ae567a96dbab22823d045ffebe0d6b14b9b799925e9ca9274d47d26ff482a6"
)
workdir="musl-fts-${version}"
useconfigure='true'

pre_configure() {
    pushd $workdir
    ./bootstrap.sh
    popd
}
