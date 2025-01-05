#!/usr/bin/env -S bash ../.port_include.sh
port='file'
version='5.46'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "http://ftp.astron.com/pub/file/file-${version}.tar.gz#c9cc77c7c560c543135edc555af609d5619dbef011997e988ce40a3d75d86088"
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
