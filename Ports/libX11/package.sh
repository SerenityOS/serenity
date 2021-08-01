#!/usr/bin/env -S bash ../.port_include.sh
port=libX11
version=1.7.2
useconfigure=true
configopts="--disable-malloc0returnsnull"
files="https://www.x.org/releases/individual/lib/libX11-${version}.tar.gz libX11-${version}.tar.gz 2c26ccd08f43a6214de89110554fbe97c71692eeb7e7d4829f3004ae6fafd2c0"
depends="xproto xextproto xtrans libxcb inputproto kbproto"
auth_type="sha256"
