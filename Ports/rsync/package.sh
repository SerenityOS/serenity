#!/usr/bin/env -S bash ../.port_include.sh
port='rsync'
version='3.2.7'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz#4e7d9d3f6ed10878c58c5fb724a67dacf4b6aac7340b13e488fb2dc41346f2bb"
)
configopts=(
    "--target=${SERENITY_ARCH}-pc-serenity"
    '--disable-xxhash'
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
