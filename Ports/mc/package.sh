#!/usr/bin/env -S bash ../.port_include.sh
port='mc'
version='4.8.29'
useconfigure='true'
files=(
    "http://ftp.midnight-commander.org/mc-${version}.tar.xz 01d8a3b94f58180cca5bf17257b5078d1fd6fd27a9b5c0e970ec767549540ad4"
)
depends=(
    'gettext'
    'glib'
    'ncurses'
    'vim'
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
    '--disable-largefile'
    '--disable-vfs'
    '--without-edit'
    '--without-x'
    '--with-homedir'
    '--with-screen=ncurses'
)
use_fresh_config_sub='true'
config_sub_paths=(
    'config/config.sub'
)
