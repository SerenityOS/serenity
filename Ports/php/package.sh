#!/usr/bin/env -S bash ../.port_include.sh
port=php
useconfigure="true"
version="8.0.6"
files="https://www.php.net/distributions/php-${version}.tar.xz php-${version}.tar.xz e9871d3b6c391fe9e89f86f6334852dcc10eeaaa8d5565beb8436e7f0cf30e20"
auth_type=sha256
depends="libiconv libxml2 openssl sqlite zlib"
configopts="
    --disable-opcache
    --prefix=${SERENITY_INSTALL_ROOT}/usr/local
    --with-iconv=${SERENITY_INSTALL_ROOT}/usr/local
    --with-openssl
    --with-zlib
    --without-pcre-jit
"

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2/"
export LIBS="-ldl"
export LIBXML_CFLAGS="y"
export LIBXML_LIBS="-lxml2"
export OPENSSL_CFLAGS="y"
export OPENSSL_LIBS="-lssl -lcrypto"
export SQLITE_CFLAGS="y"
export SQLITE_LIBS="-lsqlite3 -lpthread"
export ZLIB_CFLAGS="y"
export ZLIB_LIBS="-lz"
