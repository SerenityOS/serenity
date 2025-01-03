#!/usr/bin/env -S bash ../.port_include.sh
port='cpio'
version='2.15'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('build-aux/config.sub')
files=(
    "https://ftpmirror.gnu.org/gnu/cpio/cpio-${version}.tar.gz#efa50ef983137eefc0a02fdb51509d624b5e3295c980aa127ceee4183455499e"
)
