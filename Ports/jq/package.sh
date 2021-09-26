#!/usr/bin/env -S bash ../.port_include.sh
port=jq
version=1.6
useconfigure=true
configopts=("--with-oniguruma=builtin" "--disable-maintainer-mode")
files="https://github.com/stedolan/jq/releases/download/jq-${version}/jq-${version}.tar.gz jq-${version}.tar.gz 5de8c8e29aaa3fb9cc6b47bb27299f271354ebb72514e3accadc7d38b5bbaa72"
auth_type=sha256
makeopts=("LDFLAGS=-all-static")

pre_configure() {
    pushd $workdir/modules/oniguruma
    autoreconf -fi
    rm config.sub
    cp ../../config/config.sub config.sub
    popd
}
