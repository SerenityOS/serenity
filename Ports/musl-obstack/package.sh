#!/usr/bin/env -S bash ../.port_include.sh
port='musl-obstack'
version='1.2.3'
useconfigure='true'
files="https://github.com/void-linux/musl-obstack/archive/refs/tags/v${version}.zip musl-obstack-${version}.zip ec4d5943d4531a3f330966d3fcb5aaa4cc80bab611af4b88139608ffbbc75263"
auth_type='sha256'

pre_configure() {
    run ./bootstrap.sh
}
