#!/usr/bin/env -S bash ../.port_include.sh

port="ObjFW"
version="master"
commit="2903ecda7767a9563b6d3c74581b3920d32e6576"
useconfigure="true"
files=(
    "https://github.com/ObjFW/ObjFW/archive/${commit}.tar.gz ObjFW-${commit}.tar.gz ef5e3158e898415a9458f2c9a620b47f111b9a2af0bc8c48bcde4e13ae2b7727"
)
workdir="ObjFW-${commit}"
use_fresh_config_sub='true'
config_sub_paths=("build-aux/config.sub")
depends=("openssl")

# Only required for non-releases.
pre_configure() {
    run ./autogen.sh
}
