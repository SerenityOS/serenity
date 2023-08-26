#!/usr/bin/env -S bash ../.port_include.sh
port='patchutils'
version='0.4.2'
useconfigure='true'
files=(
    "https://github.com/twaugh/patchutils/archive/refs/tags/${version}.tar.gz 2ff95f11946558ce63f4d1167abaccbffd49750152346d5304e03ad884304ad6"
)

pre_configure() {
    run ./bootstrap
}
