#!/usr/bin/env -S bash ../.port_include.sh
port=dropbear
version=2022.82
files=(
    "https://mirror.dropbear.nl/mirror/releases/dropbear-${version}.tar.bz2#3a038d2bbc02bf28bbdd20c012091f741a3ec5cbe460691811d714876aad75d1"
)
useconfigure=true
use_fresh_config_sub=true
# don't care about zlib, less deps is better
configopts=("--disable-zlib")
# Serenity's utmp is not fully compatible with what dropbear expects.
configopts+=("--disable-utmp" "--disable-wtmp" "--disable-login" "--disable-lastlog")
