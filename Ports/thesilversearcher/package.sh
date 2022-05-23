#!/usr/bin/env -S bash ../.port_include.sh
port=thesilversearcher
version=2.2.0
useconfigure="true"
files="https://github.com/ggreer/the_silver_searcher/archive/refs/tags/${version}.tar.gz the_silver_searcher-${version}.tar.xz 6a0a19ca5e73b2bef9481c29a508d2413ca1a0a9a5a6b1bd9bbd695a7626cbf9"
workdir="the_silver_searcher-${version}"
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--disable-utf8")
depends=("pcre" "xz")
auth_type="sha256"

pre_configure() {
    export CFLAGS="-fcommon -D_GNU_SOURCE -lpthread"
    run aclocal
    run autoconf
    run autoheader
    run automake --add-missing
}
