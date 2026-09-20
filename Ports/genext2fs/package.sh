#!/usr/bin/env -S bash ../.port_include.sh
port='genext2fs'
version='1.6.3'
files=(
    "https://github.com/bestouff/genext2fs/archive/v${version}.tar.gz#e3503a5bae3fd4b5b2c2d4f49b5b7f8d08e7accb20ab28c0f9647389b2c8a079"
)
useconfigure='true'

pre_patch() {
    run ./autogen.sh
}
