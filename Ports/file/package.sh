#!/usr/bin/env -S bash ../.port_include.sh
port='file'
version='5.48'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "http://ftp.astron.com/pub/file/file-${version}.tar.gz#ed14656883b23a364b4057c05595d93252da9bc473d30106519519d0da141283"
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
    export LD_LIBRARY_PATH="${PORT_BUILD_DIR}/host-build/src/.libs/:$LD_LIBRARY_PATH"
    run make
}
