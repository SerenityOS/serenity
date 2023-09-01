#!/usr/bin/env -S bash ../.port_include.sh
port='less'
version='643'
useconfigure='true'
files=(
    "https://ftpmirror.gnu.org/gnu/less/less-${version}.tar.gz#2911b5432c836fa084c8a2e68f6cd6312372c026a58faaa98862731c8b6052e8"
)
depends=(
    'ncurses'
)

post_configure() {
    run_replace_in_file 's/#define HAVE_WCTYPE 1/\/* #undef HAVE_WCTYPE *\//' defines.h
    run touch stamp-h # prevent config.status from overwriting our changes
}
