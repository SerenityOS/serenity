#!/usr/bin/env -S bash ../.port_include.sh
port='genext2fs'
version='1.5.0'
files="https://github.com/bestouff/genext2fs/archive/v${version}.tar.gz genext2fs-${version}.tar.xz d3861e4fe89131bd21fbd25cf0b683b727b5c030c4c336fadcd738ada830aab0"
auth_type='sha256'
useconfigure='true'

pre_patch() {
    run ./autogen.sh
}
