#!/usr/bin/env -S bash ../.port_include.sh
port=libgd
version=2.3.3
useconfigure=true
files="https://github.com/libgd/libgd/releases/download/gd-${version}/libgd-${version}.tar.gz libgd-${version}.tar.gz"
depends=("libpng")
