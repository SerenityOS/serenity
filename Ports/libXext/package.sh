#!/usr/bin/env -S bash ../.port_include.sh
port=libXext
version=1.3.4
useconfigure=true
configopts="--disable-malloc0returnsnull"
files="https://www.x.org/releases/individual/lib/libXext-${version}.tar.gz libXext-${version}.tar.gz 8ef0789f282826661ff40a8eef22430378516ac580167da35cc948be9041aac1"
depends="xproto libX11 xextproto"
auth_type="sha256"
