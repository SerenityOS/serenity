#!/usr/bin/env -S bash ../.port_include.sh
port='rsync'
version='3.5.0'
useconfigure='true'
files=(
    "https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz#c7ffd1ef653e99540f661e47cb00b7f9cad1ee6b972399b16f93d672656e0d33"
)
configopts=(
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
