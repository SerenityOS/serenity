#!/usr/bin/env -S bash ../.port_include.sh
port='rsync'
version='3.3.0'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz#7399e9a6708c32d678a72a63219e96f23be0be2336e50fd1348498d07041df90"
)
configopts=(
    "--target=${SERENITY_ARCH}-pc-serenity"
    '--disable-xxhash'
)
depends=(
    'zstd'
)
if [ "${SERENITY_TOOLCHAIN}" = 'Clang' ]; then
    depends=(
        'lz4'
        'openssl'
    )
else
    configopts+=(
        '--disable-lz4'
        '--disable-openssl'
    )
fi
