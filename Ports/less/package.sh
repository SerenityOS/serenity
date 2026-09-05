#!/usr/bin/env -S bash ../.port_include.sh
port='less'
version='704'
useconfigure='true'
files=(
    "mirror://gnu/less/less-${version}.tar.gz#20a0b0a2bb2525fa53c7eee9beb854b4c9cf172eabb209af7020743547bfe9fb"
)
depends=(
    'ncurses'
)

post_configure() {
    run_replace_in_file 's/#define HAVE_WCTYPE 1/\/* #undef HAVE_WCTYPE *\//' defines.h
    run touch stamp-h # prevent config.status from overwriting our changes
}
