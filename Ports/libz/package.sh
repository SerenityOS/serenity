#!/bin/bash ../.port_include.sh
port=libz
version=1.2.11
useconfigure=true
files="https://www.zlib.net/zlib-1.2.11.tar.gz zlib-1.2.11.tar.gz"
workdir=zlib-1.2.11/

configure() {
    run ./configure
}
