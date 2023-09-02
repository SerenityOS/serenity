#!/usr/bin/env -S bash ../.port_include.sh
port='deutex'
version='5.2.2'
useconfigure='true'
files=(
    "https://github.com/Doom-Utils/deutex/releases/download/v${version}/deutex-${version}.tar.zst#10ed0e7a533ec97cb6d03548d4258fbec88852a45b5ea4cf5434376ad4174b5f"
)
depends=(
    'libpng'
)
