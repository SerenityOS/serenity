#!/usr/bin/env -S bash ../.port_include.sh
port=php
useconfigure="true"
version="8.1.4"
files="https://www.php.net/distributions/php-${version}.tar.xz php-${version}.tar.xz 05a8c0ac30008154fb38a305560543fc172ba79fb957084a99b8d3b10d5bdb4b"
auth_type=sha256
depends=("libiconv" "libxml2" "openssl" "readline" "sqlite" "zlib")
configopts=(
    "--disable-cgi"
    "--disable-opcache"
    "--enable-fpm"
    "--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "--with-iconv=${SERENITY_INSTALL_ROOT}/usr/local"
    "--with-openssl"
    "--with-readline=${SERENITY_INSTALL_ROOT}/usr/local"
    "--with-zlib"
    "--without-pcre-jit"
)
launcher_name="PHP"
launcher_category="Development"
launcher_command="/usr/local/bin/php -a"
launcher_run_in_terminal="true"
icon_file="win32/build/php.ico"

pre_configure() {
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

    run ./buildconf --force
}

post_configure() {
    unset ZLIB_LIBS
    unset ZLIB_CFLAGS
    unset SQLITE_LIBS
    unset SQLITE_CFLAGS
    unset OPENSSL_LIBS
    unset OPENSSL_CFLAGS
    unset LIBXML_LIBS
    unset LIBS
    unset LIBXML_CFLAGS
    unset CFLAGS
}
