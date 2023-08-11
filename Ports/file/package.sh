#!/usr/bin/env -S bash ../.port_include.sh
port='file'
version='5.45'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "http://ftp.astron.com/pub/file/file-${version}.tar.gz#fc97f51029bb0e2c9f4e3bffefdaf678f0e039ee872b9de5c002a6d09c784d82"
)

function pre_configure() {
    host_env
    mkdir -p "host-build"
    (
        cd host-build
        "../${workdir}/configure"
        make
    )
}

function build() {
    export PATH="${PORT_BUILD_DIR}/host-build/src/.libs/:$PATH"
    run make
}
