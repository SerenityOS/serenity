#!/usr/bin/env -S bash ../.port_include.sh
port='file'
version='5.44'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('config.sub')
files=(
    "http://ftp.astron.com/pub/file/file-${version}.tar.gz#3751c7fba8dbc831cb8d7cc8aff21035459b8ce5155ef8b0880a27d028475f3b"
)
