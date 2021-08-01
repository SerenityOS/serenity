#!/usr/bin/env -S bash ../.port_include.sh
port=xeyes
version=1.1.2
useconfigure=true
files="https://www.x.org/releases/individual/app/xeyes-${version}.tar.gz xeyes-${version}.tar.gz 4a675b34854da362bd8dff4f21ff92e0c19798b128ea0af24b7fc7c5ac2feea3"
depends="libX11 libXt libXext libXmu xproto libXrender"
auth_type="sha256"

pre_configure() {
    # We updated configure.ac, so reconfigure.
    run cp "config.sub" "config.sub.bak"
    run autoreconf -vfi
    run mv "config.sub.bak" "config.sub"
}

export CFLAGS='-lpthread'
