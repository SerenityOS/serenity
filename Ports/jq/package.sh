#!/bin/bash ../.port_include.sh
port=jq
version=1.6
useconfigure=true
configopts="--with-oniguruma=builtin --disable-maintainer-mode"
files="https://github.com/stedolan/jq/releases/download/jq-1.6/jq-1.6.tar.gz jq-1.6.tar.gz"
makeopts="LDFLAGS=-all-static"

build() {
    run make $makeopts
}

pre_configure() {
    pushd $workdir/modules/oniguruma
    autoreconf -fi
    rm config.sub
    cp ../../config/config.sub config.sub
    popd
}
