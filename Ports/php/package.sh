#!/usr/bin/env -S bash ../.port_include.sh
port='php'
useconfigure='true'
version='8.2.10'
files=(
    "https://www.php.net/distributions/php-${version}.tar.xz#561dc4acd5386e47f25be76f2c8df6ae854756469159248313bcf276e282fbb3"
)
depends=(
    'curl'
    'libiconv'
    'libxml2'
    'openssl'
    'readline'
    'sqlite'
    'zlib'
)
configopts=(
    '--disable-cgi'
    '--disable-opcache'
    '--enable-fpm'
    "--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    '--with-curl'
    "--with-iconv=${SERENITY_INSTALL_ROOT}/usr/local"
    '--with-openssl'
    "--with-readline=${SERENITY_INSTALL_ROOT}/usr/local"
    '--with-zlib'
    '--without-pcre-jit'
)
launcher_name='PHP'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/php -a'
launcher_run_in_terminal='true'
icon_file='win32/build/php.ico'

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/include/LibCrypt -I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2"
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
