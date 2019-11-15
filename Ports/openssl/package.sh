#!/bin/bash ../.port_include.sh
port=openssl
version=1.0.2t
useconfigure=true
configscript=Configure
files="https://www.openssl.org/source/openssl-1.0.2t.tar.gz openssl-1.0.2t.tar.gz "
depends="zlib"
usr_local=$SERENITY_ROOT/Root/usr/local/
configopts="--prefix=$usr_local --openssldir=$usr_local/ssl BSD-x86 zlib no-tests no-threads no-asm no-sock"

configure() {
    run rm -rf ./test/ ./apps/
    run ./"$configscript" $configopts
}
