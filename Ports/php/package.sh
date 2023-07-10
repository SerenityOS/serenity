#!/usr/bin/env -S bash ../.port_include.sh
port='php'
useconfigure='true'
version='8.1.12'
files=(
    "https://www.php.net/distributions/php-${version}.tar.xz php-${version}.tar.xz 08243359e2204d842082269eedc15f08d2eca726d0e65b93fb11f4bfc51bbbab"
)
depends=(
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
    "--with-iconv=${SERENITY_INSTALL_ROOT}/usr/local"
    '--with-openssl'
    "--with-readline=${SERENITY_INSTALL_ROOT}/usr/local"
    '--with-zlib'
    '--without-pcre-jit'
)
launcher_name='PHP'
launcher_category='Development'
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
