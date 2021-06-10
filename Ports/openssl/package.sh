#!/usr/bin/env -S bash ../.port_include.sh
port=openssl
branch='1.0.2'
version="${branch}t"
useconfigure=true
configscript=Configure
files="https://ftp.openssl.org/source/old/${branch}/openssl-${version}.tar.gz openssl-${version}.tar.gz 14cb464efe7ac6b54799b34456bd69558a749a4931ecfd9cf9f71d7881cac7bc"
auth_type=sha256

depends="zlib"
configopts="--prefix=/usr/local --install_prefix=${SERENITY_INSTALL_ROOT} -DOPENSSL_SYS_SERENITY=1 linux-elf zlib no-tests no-threads no-asm"

configure() {
    run ./"$configscript" $configopts
}
