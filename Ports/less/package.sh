#!/usr/bin/env -S bash ../.port_include.sh
port='less'
version='710'
useconfigure='true'
files=(
    "mirror://gnu/less/less-${version}.tar.gz#d1008fb78dcae1323ddab664bcb352a61f022b1b131bd8018548e021d975ec7a"
)
depends=(
    'ncurses'
)

post_configure() {
    run_replace_in_file 's/#define HAVE_WCTYPE 1/\/* #undef HAVE_WCTYPE *\//' defines.h
    run touch stamp-h # prevent config.status from overwriting our changes
}
