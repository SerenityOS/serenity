#!/usr/bin/env -S bash ../.port_include.sh
port='unibilium'
version='2.1.2'
files=(
    "https://github.com/neovim/unibilium/archive/refs/tags/v${version}.tar.gz#370ecb07fbbc20d91d1b350c55f1c806b06bf86797e164081ccc977fc9b3af7a"
)
useconfigure='true'

pre_configure() {
    run autoreconf -i
}
