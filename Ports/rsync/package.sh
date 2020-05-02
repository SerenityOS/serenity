#!/bin/bash ../.port_include.sh
port=rsync
version=3.1.3
useconfigure="true"
files="https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz rsync-${version}.tar.gz
https://download.samba.org/pub/rsync/src/rsync-${version}.tar.gz.asc rsync-${version}.tar.gz.asc"
configopts="--target=i686-pc-serenity"