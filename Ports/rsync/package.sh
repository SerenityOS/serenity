#!/usr/bin/env -S bash ../.port_include.sh
port=rsync
version=3.1.3
useconfigure="true"
files="https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz rsync-${version}.tar.gz 1581a588fde9d89f6bc6201e8129afaf
https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz.asc rsync-${version}.tar.gz.asc 5cde11b63857d647f6cb9850f76c52cb"
auth_type=md5
configopts="--target=${SERENITY_ARCH}-pc-serenity"
