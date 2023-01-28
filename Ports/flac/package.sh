#!/usr/bin/env -S bash ../.port_include.sh
port='flac'
description='Free Lossless Audio Codec'
version='1.4.2'
website='https://xiph.org/flac/'
auth_type='sha256'
useconfigure='true'
depends=('libogg')
files="https://downloads.xiph.org/releases/flac/flac-${version}.tar.xz flac-${version}.tar.xz e322d58a1f48d23d9dd38f432672865f6f79e73a6f9cc5a5f57fcaa83eb5a8e4"
