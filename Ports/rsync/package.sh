#!/usr/bin/env -S bash ../.port_include.sh
port='rsync'
version='3.4.1'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz#2924bcb3a1ed8b551fc101f740b9f0fe0a202b115027647cf69850d65fd88c52"
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
