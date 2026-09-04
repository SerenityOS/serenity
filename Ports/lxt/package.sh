#!/usr/bin/env -S bash ../.port_include.sh
port='lxt'
version='1.3c'
files=(
    "http://stahlke.org/dan/lxt/lxt-${version}.tar.gz#44d3e50c88c42be3fdd7c31b39cf087dde73c9e2bfdf42bfea506a81e92a5427"
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
    run ./configure --host=x86_64-serenity
}
