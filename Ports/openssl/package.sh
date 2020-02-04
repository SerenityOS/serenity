#!/bin/bash ../.port_include.sh
port=openssl
version=1.0.2t
useconfigure=true
configscript=Configure
files="https://www.openssl.org/source/openssl-${version}.tar.gz openssl-${version}.tar.gz
https://www.openssl.org/source/openssl-${version}.tar.gz.asc openssl-${version}.tar.gz.asc"
auth_type="sig"
auth_import_key="8657ABB260F056B1E5190839D9C4D26D0E604491"
auth_opts="openssl-${version}.tar.gz.asc openssl-${version}.tar.gz"

depends="zlib"
usr_local=$SERENITY_ROOT/Root/usr/local/
configopts="--prefix=$usr_local --openssldir=$usr_local/ssl BSD-x86 zlib no-tests no-threads no-asm no-sock"

configure() {
    run rm -rf ./test/ ./apps/
    run ./"$configscript" $configopts
}
