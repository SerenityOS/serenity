#!/usr/bin/env -S bash ../.port_include.sh
port=php
useconfigure="true"
version="8.1.1"
files="https://www.php.net/distributions/php-${version}.tar.xz php-${version}.tar.xz 33c09d76d0a8bbb5dd930d9dd32e6bfd44e9efcf867563759eb5492c3aff8856"
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
