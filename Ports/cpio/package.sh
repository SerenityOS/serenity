#!/usr/bin/env -S bash ../.port_include.sh
port='cpio'
version='2.13'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('build-aux/config.sub')
files=(
    "https://ftpmirror.gnu.org/gnu/cpio/cpio-${version}.tar.gz e87470d9c984317f658567c03bfefb6b0c829ff17dbf6b0de48d71a4c8f3db88"
)
