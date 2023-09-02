#!/usr/bin/env -S bash ../.port_include.sh
port='tar'
version='1.35'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('build-aux/config.sub')
files=(
    "https://ftpmirror.gnu.org/gnu/tar/tar-${version}.tar.gz#14d55e32063ea9526e057fbf35fcabd53378e769787eff7919c3755b02d2b57e"
)
depends=(
    'gettext'
)
configopts=(
    "--without-selinux"
    "--without-posix-acls"
    "--without-xattrs"
)
