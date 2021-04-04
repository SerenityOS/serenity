#!/usr/bin/env -S bash ../.port_include.sh
port=faad2
version=2_10_0
useconfigure=true
files="https://github.com/knik0/faad2/archive/refs/tags/${version}.tar.gz faad2-${version}.tar.gz"

post_fetch() {
    cd "faad2-${version}"
    autoreconf -vif
    cd ..
}
