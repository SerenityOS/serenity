#!/usr/bin/env -S bash ../.port_include.sh
port=libxcb
version=1.14
useconfigure=true
files="https://www.x.org/releases/individual/xcb/libxcb-${version}.tar.gz libxcb-${version}.tar.gz 2c7fcddd1da34d9b238c9caeda20d3bd7486456fc50b3cc6567185dbd5b0ad02"
depends="xcb-proto libXau libpthread-stubs"
auth_type="sha256"

export PYTHON=python2
