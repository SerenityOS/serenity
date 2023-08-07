#!/usr/bin/env -S bash ../.port_include.sh

port='bdwgc'
version='8.2.2'
use_fresh_config_sub='true'
files=(
    "https://github.com/ivmai/bdwgc/releases/download/v$version/gc-$version.tar.gz gc-$version.tar.gz f30107bcb062e0920a790ffffa56d9512348546859364c23a14be264b38836a0"
)
depends=("libatomic_ops")
workdir="gc-$version"

useconfigure='true'
configopts=("--enable-threads=posix")
pre_configure() {
    run ./autogen.sh
}
