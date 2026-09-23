#!/usr/bin/env -S bash ../.port_include.sh
port='rsync'
version='3.5.1'
useconfigure='true'
files=(
    "https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz#c55f9c9dc10fb8bec397b399a0fdded53cc9a2d8e30891bb0d63724d25c37bef"
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
