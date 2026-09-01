#!/usr/bin/env -S bash ../.port_include.sh
port='joe'
version='4.8'
files=(
    "https://downloads.sourceforge.net/joe-editor/joe-${version}.tar.gz#6995b28ee20dcdbbcb5a45a4c110642dc96d67748aea27450c74cdb4dd07cc20"
)
useconfigure="true"
configopts=(
    "--disable-curses"
    "--disable-termcap"
)
