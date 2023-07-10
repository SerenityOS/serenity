#!/usr/bin/env -S bash ../.port_include.sh
port=less
version=590
useconfigure="true"
files=(
    "https://ftpmirror.gnu.org/gnu/less/less-${version}.tar.gz less-${version}.tar.gz 6aadf54be8bf57d0e2999a3c5d67b1de63808bb90deb8f77b028eafae3a08e10"
)
depends=("ncurses")

post_configure() {
    run_replace_in_file "s/#define HAVE_WCTYPE 1/\/* #undef HAVE_WCTYPE *\//" defines.h
    run touch stamp-h # prevent config.status from overwriting our changes
}
