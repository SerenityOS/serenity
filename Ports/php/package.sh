#!/usr/bin/env -S bash ../.port_include.sh
port='php'
useconfigure='true'
version='8.1.11'
files="https://www.php.net/distributions/php-${version}.tar.xz php-${version}.tar.xz 3005198d7303f87ab31bc30695de76e8ad62783f806b6ab9744da59fe41cc5bd"
auth_type='sha256'
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

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2"
export LIBS='-ldl'
export LIBXML_CFLAGS='y'
export LIBXML_LIBS='-lxml2'
export OPENSSL_CFLAGS='y'
export OPENSSL_LIBS='-lssl -lcrypto'
export SQLITE_CFLAGS='y'
export SQLITE_LIBS='-lsqlite3 -lpthread'
export ZLIB_CFLAGS='y'
export ZLIB_LIBS='-lz'

pre_configure() {
    run ./buildconf --force
}
