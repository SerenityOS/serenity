#!/usr/bin/env -S bash ../.port_include.sh
port='gpgme'
version='2.2.0'
useconfigure='true'
files=(
    "https://gnupg.org/ftp/gcrypt/gpgme/gpgme-${version}.tar.bz2#7160e80e84dafd00d956c84891c533bb7ab16a6a54fbe1574b2f3acf0496977b"
)
depends=(
    'gnupg'
)
configopts=(
    '--disable-gpg-test'
    "--with-libgpg-error-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "--with-libassuan-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
