#!/usr/bin/env -S bash ../.port_include.sh
port=xz
version=5.2.5
useconfigure=true
configopts=("--disable-static" "--enable-shared")
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
files=(
    "https://tukaani.org/xz/xz-${version}.tar.gz#f6f4910fd033078738bd82bfba4f49219d03b17eb0794eb91efbae419f4aba10"
)
depends=("zlib" "libiconv")
