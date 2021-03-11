#!/usr/bin/env -S bash ../.port_include.sh
port=jq
version=1.6
useconfigure=true
configopts="--with-oniguruma=builtin --disable-maintainer-mode"
files="https://github.com/stedolan/jq/releases/download/jq-${version}/jq-${version}.tar.gz jq-${version}.tar.gz"
makeopts="LDFLAGS=-all-static"

pre_configure() {
    pushd $workdir/modules/oniguruma
    autoreconf -fi
    rm config.sub
    cp ../../config/config.sub config.sub
    popd
}
