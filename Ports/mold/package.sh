#!/usr/bin/env -S bash ../.port_include.sh
port=mold
version=1.0.3
files="https://github.com/rui314/mold/archive/refs/tags/v${version}.tar.gz mold-${version}.tgz 488c12058b4c7c77bff94c6f919e40b2f12c304214e2e0d7d4833c21167837c0"
auth_type=sha256
depends=("zlib" "openssl")
makeopts=("OS=SerenityOS" "LDFLAGS=-L${DESTDIR}/usr/local/lib" "-j$(nproc)")
installopts=("OS=SerenityOS")
