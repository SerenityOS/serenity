#!/usr/bin/env -S bash ../.port_include.sh
port=php
useconfigure="true"
version="8.0.8"
files="https://www.php.net/distributions/php-${version}.tar.xz php-${version}.tar.xz dc1668d324232dec1d05175ec752dade92d29bb3004275118bc3f7fc7cbfbb1c"
auth_type=sha256
depends="libiconv libxml2 openssl readline sqlite zlib"
configopts="
    --disable-opcache
    --prefix=${SERENITY_INSTALL_ROOT}/usr/local
    --with-iconv=${SERENITY_INSTALL_ROOT}/usr/local
    --with-openssl
    --with-readline=${SERENITY_INSTALL_ROOT}/usr/local
    --with-zlib
    --without-pcre-jit
"

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2"
export LIBS="-ldl"
export LIBXML_CFLAGS="y"
export LIBXML_LIBS="-lxml2"
export OPENSSL_CFLAGS="y"
export OPENSSL_LIBS="-lssl -lcrypto"
export SQLITE_CFLAGS="y"
export SQLITE_LIBS="-lsqlite3 -lpthread"
export ZLIB_CFLAGS="y"
export ZLIB_LIBS="-lz"
