#!/usr/bin/env -S bash ../.port_include.sh
port=jq
version=1.6
useconfigure=true
configopts=("--with-oniguruma=builtin" "--disable-maintainer-mode")
files=(
    "https://github.com/stedolan/jq/releases/download/jq-${version}/jq-${version}.tar.gz#5de8c8e29aaa3fb9cc6b47bb27299f271354ebb72514e3accadc7d38b5bbaa72"
)
makeopts=("LDFLAGS=-all-static")
use_fresh_config_sub=true
config_sub_paths=("config/config.sub" "modules/oniguruma/config.sub")

pre_configure() {
    # FIXME: The maintainer forgot to run autoconf before creating the tarball.
    # Remove this when the next update is released.
    pushd $workdir/modules/oniguruma
    autoreconf -i
    popd
}
