#!/bin/bash ../.port_include.sh
port=zlib
version=1.2.11
useconfigure=true
files="https://www.zlib.net/zlib-1.2.11.tar.gz zlib-1.2.11.tar.gz"

configure() {
    run ./configure --static
}
