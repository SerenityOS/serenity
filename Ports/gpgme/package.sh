#!/usr/bin/env -S bash ../.port_include.sh
port='gpgme'
version='1.23.2'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
files=(
    "https://gnupg.org/ftp/gcrypt/gpgme/gpgme-${version}.tar.bz2#9499e8b1f33cccb6815527a1bc16049d35a6198a6c5fae0185f2bd561bce5224"
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

post_install() {
    run sed -i 's#/usr/local#${CMAKE_SYSROOT}/usr/local#g' ${SERENITY_INSTALL_ROOT}/usr/local/lib/cmake/Gpgmepp/GpgmeppConfig.cmake
}
