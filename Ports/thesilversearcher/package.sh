#!/usr/bin/env -S bash ../.port_include.sh
port=thesilversearcher
version=2.2.0
useconfigure="true"
files=(
    "https://geoff.greer.fm/ag/releases/the_silver_searcher-${version}.tar.gz#d9621a878542f3733b5c6e71c849b9d1a830ed77cb1a1f6c2ea441d4b0643170"
)
workdir="the_silver_searcher-${version}"
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--disable-utf8")
depends=("pcre" "xz")
use_fresh_config_sub=true

export CFLAGS="-fcommon -D_GNU_SOURCE -lpthread"
