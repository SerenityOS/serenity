#!/usr/bin/env -S bash ../.port_include.sh
port=libXrender
version=0.9.10
useconfigure=true
configopts="--disable-malloc0returnsnull"
files="https://www.x.org/releases/individual/lib/libXrender-${version}.tar.gz libXrender-${version}.tar.gz 770527cce42500790433df84ec3521e8bf095dfe5079454a92236494ab296adf"
depends="libX11 renderproto"
auth_type="sha256"
