#!/usr/bin/env -S bash ../.port_include.sh
port='flac'
version='1.4.2'
useconfigure='true'
depends=('libogg')
files=(
    "https://downloads.xiph.org/releases/flac/flac-${version}.tar.xz e322d58a1f48d23d9dd38f432672865f6f79e73a6f9cc5a5f57fcaa83eb5a8e4"
)
