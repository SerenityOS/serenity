#!/usr/bin/env -S bash ../.port_include.sh
port=mold
version=1.0.2
files="https://github.com/rui314/mold/archive/refs/tags/v${version}.tar.gz mold-${version}.tgz 1a5c4779d10c6c81d21092ea776504f51e6a4994121f536550c60a8e7bb6a028"
auth_type=sha256
depends=("zlib" "openssl")
makeopts=("OS=SerenityOS" "LDFLAGS=-L${DESTDIR}/usr/local/lib" "-j$(nproc)")
installopts=("OS=SerenityOS")
