#!/bin/bash ../.port_include.sh
port=dropbear
version=2019.78
files="https://matt.ucc.asn.au/dropbear/releases/dropbear-${version}.tar.bz2 dropbear-${version}.tar.bz2
https://matt.ucc.asn.au/dropbear/releases/dropbear-${version}.tar.bz2.asc dropbear-${version}.tar.bz2.asc
https://matt.ucc.asn.au/dropbear/releases/dropbear-key-2015.asc dropbear-key-2015.asc"

auth_type="sig"
auth_opts="--keyring ./dropbear-key-2015.asc dropbear-${version}.tar.bz2.asc"
useconfigure=true
# don't care about zlib, less deps is better
configopts="--disable-zlib "
# Serenity's utmp is not fully compatible with what dropbear expects.
configopts+="--disable-utmp --disable-wtmp --disable-login --disable-lastlog "
# not added automatically
configopts+="--enable-static"
