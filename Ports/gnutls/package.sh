#!/usr/bin/env -S bash ../.port_include.sh
port='gnutls'
version='3.6.16'
use_fresh_config_sub='true'
use_fresh_config_guess='true'
config_sub_paths=("build-aux/config.sub")
config_guess_paths=("build-aux/config.guess")
useconfigure='true'
files=(
    "https://www.gnupg.org/ftp/gcrypt/gnutls/v3.6/gnutls-${version}.tar.xz 1b79b381ac283d8b054368b335c408fedcb9b7144e0c07f531e3537d4328f3b3"
)
depends=(
    'nettle'
    'gmp'
    'libtasn1'
    'libunistring'
)
configopts=(
    '--without-p11-kit'
    '--disable-guile'
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
    '--disable-tests'
    '--enable-local-libopts'
    '--disable-full-test-suite'
)
