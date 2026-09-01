#!/usr/bin/env -S bash ../.port_include.sh
port='deutex'
version='5.2.3'
useconfigure='true'
files=(
    "https://github.com/Doom-Utils/deutex/releases/download/v${version}/deutex-${version}.tar.zst#935dcae490fb574e8ad90ef54bdeb599c7055fedca117d79ce837cbc19d070ab"
)
depends=(
    'libpng'
)
