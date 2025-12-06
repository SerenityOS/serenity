#!/usr/bin/env -S bash ../.port_include.sh
port='php'
useconfigure='true'
version='8.5.0'
files=(
    "https://www.php.net/distributions/php-${version}.tar.xz#39cb6e4acd679b574d3d3276f148213e935fc25f90403eb84fb1b836a806ef1e"
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
    '--disable-opcache-jit'
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
