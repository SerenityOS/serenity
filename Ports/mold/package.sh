#!/usr/bin/env -S bash ../.port_include.sh
port=mold
version=1.0.1
files="https://github.com/rui314/mold/archive/refs/tags/v${version}.tar.gz mold-${version}.tgz b0d54602d1229c26583ee8a0132e53463c4d755f9dbc456475f388fd8a1aa3e4"
auth_type=sha256
depends=("zlib" "openssl")
makeopts=("OS=SerenityOS" "EXTRA_LDFLAGS=-L${DESTDIR}/usr/local/lib -lcore" "-j$(nproc)")
installopts=("OS=SerenityOS")
