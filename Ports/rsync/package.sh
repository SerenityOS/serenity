#!/usr/bin/env -S bash ../.port_include.sh
port='rsync'
version='3.5.2'
useconfigure='true'
files=(
    "https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz#a10d591825251e070f74c98df3daad195def4ed81b4a2de16169026aea33f6c8"
)
configopts=(
    '--disable-idn'
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
