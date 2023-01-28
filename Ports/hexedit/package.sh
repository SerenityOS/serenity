#!/usr/bin/env -S bash ../.port_include.sh
port='hexedit'
description='A console-based hex editor'
version='1.6'
website='https://github.com/pixel/hexedit'
files="https://github.com/pixel/hexedit/archive/refs/tags/${version}.tar.gz hexedit-${version}.tar.gz 598906131934f88003a6a937fab10542686ce5f661134bc336053e978c4baae3"
auth_type='sha256'
depends=("ncurses")
useconfigure='true'

pre_patch() {
    run ./autogen.sh
}
