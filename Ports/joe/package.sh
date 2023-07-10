#!/usr/bin/env -S bash ../.port_include.sh
port=joe
version=4.6
files=(
    "https://sourceforge.net/projects/joe-editor/files/JOE%20sources/joe-${version}/joe-${version}.tar.gz/download joe-${version}.tar.gz 495a0a61f26404070fe8a719d80406dc7f337623788e445b92a9f6de512ab9de"
)
useconfigure="true"
configopts=(
    "--disable-curses"
    "--disable-termcap"
)
use_fresh_config_sub=true
