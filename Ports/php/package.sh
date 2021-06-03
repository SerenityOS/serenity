#!/usr/bin/env -S bash ../.port_include.sh
port=php
useconfigure="true"
version="8.0.6"
files="https://www.php.net/distributions/php-${version}.tar.xz php-${version}.tar.xz e9871d3b6c391fe9e89f86f6334852dcc10eeaaa8d5565beb8436e7f0cf30e20"
auth_type=sha256
depends=""
configopts="
    --disable-dom
    --disable-opcache
    --disable-phar
    --disable-simplexml
    --disable-xml
    --disable-xmlreader
    --disable-xmlwriter
    --prefix=${SERENITY_INSTALL_ROOT}/usr/local
    --without-iconv
    --without-libxml
    --without-pdo-sqlite
    --without-sqlite3
"

export LIBS="-ldl"
