#!/usr/bin/env -S bash ../.port_include.sh
port='kakoune'
version='e605ad8582d8e015806ed9b4d7aba8ca1ea13d57'
files=(
    "https://github.com/mawww/kakoune/archive/${version}.tar.gz#420823df611202f3c18a004def032cc9cec46c63b7754249aae2d8c1e72fb1b2"
)
depends=(
    'bash'
    'sed'
)
makeopts+=(
    "LDFLAGS=-L${DESTDIR}/usr/local/lib"
)
