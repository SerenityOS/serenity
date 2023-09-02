#!/usr/bin/env -S bash ../.port_include.sh
port='lxt'
version='1.3b'
files=(
    "http://stahlke.org/dan/lxt/lxt-${version}.tar.gz#7a3ab299a6d61a71b271fd13b847b5a1c22a5f95df78561a325c78d50b6a6bc7"
)
useconfigure='true'
depends=(
    'bash'
    'ncurses'
)

configure() {
    export ac_cv_func_fnmatch_works=yes
    export ac_cv_func_malloc_0_nonnull=yes
    export ac_cv_func_realloc_0_nonnull=yes
    run ./configure --host=x86_64-pc-serenity
}
