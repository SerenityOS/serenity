#!/usr/bin/env -S bash ../.port_include.sh
port=rsync
version=3.1.3
useconfigure="true"
use_fresh_config_sub="true"
files=(
    "https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz rsync-${version}.tar.gz 55cc554efec5fdaad70de921cd5a5eeb6c29a95524c715f3bbf849235b0800c0"
)
configopts=("--target=${SERENITY_ARCH}-pc-serenity")
