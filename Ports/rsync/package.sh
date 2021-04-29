#!/usr/bin/env -S bash ../.port_include.sh
port=rsync
version=3.1.3
useconfigure="true"
files="https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz rsync-${version}.tar.gz 55cc554efec5fdaad70de921cd5a5eeb6c29a95524c715f3bbf849235b0800c0
https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz.asc rsync-${version}.tar.gz.asc 5cde11b63857d647f6cb9850f76c52cb"
auth_type=sha256
configopts="--target=${SERENITY_ARCH}-pc-serenity"
