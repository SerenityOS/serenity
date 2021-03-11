#!/usr/bin/env -S bash ../.port_include.sh
port=openssl
branch='1.0.2'
version="${branch}t"
useconfigure=true
configscript=Configure
files="https://ftp.openssl.org/source/old/${branch}/openssl-${version}.tar.gz openssl-${version}.tar.gz
https://ftp.openssl.org/source/old/${branch}/openssl-${version}.tar.gz.asc openssl-${version}.tar.gz.asc"
auth_type="sig"
auth_import_key="8657ABB260F056B1E5190839D9C4D26D0E604491"
auth_opts="openssl-${version}.tar.gz.asc openssl-${version}.tar.gz"

depends="zlib"
configopts="--prefix=${SERENITY_BUILD_DIR}/Root/usr/local -DOPENSSL_SYS_SERENITY=1 --openssldir=${SERENITY_BUILD_DIR}/Root/usr/local/ssl BSD-x86 zlib no-tests no-threads no-asm"

configure() {
    run rm -rf ./test/
    run ./"$configscript" $configopts
}
