#!/bin/bash ../.port_include.sh
port=zlib
version=1.2.11
useconfigure=true
files="https://www.zlib.net/zlib-${version}.tar.gz zlib-${version}.tar.gz
https://www.zlib.net/zlib-${version}.tar.gz.asc zlib-${version}.tar.gz.asc"

auth_type="sig"
auth_import_key="783FCD8E58BCAFBA"
auth_opts="zlib-${version}.tar.gz.asc"

configure() {
    run ./configure --static
}
