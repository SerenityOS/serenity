#!/usr/bin/env -S bash ../.port_include.sh
port=joe
version=4.6
files=(
    "https://downloads.sourceforge.net/joe-editor/joe-${version}.tar.gz#495a0a61f26404070fe8a719d80406dc7f337623788e445b92a9f6de512ab9de"
)
useconfigure="true"
configopts=(
    "--disable-curses"
    "--disable-termcap"
)
use_fresh_config_sub=true
