#!/usr/bin/env -S bash ../.port_include.sh
port=less
version=530
useconfigure="true"
files="https://ftpmirror.gnu.org/gnu/less/less-${version}.tar.gz less-${version}.tar.gz
https://ftpmirror.gnu.org/gnu/less/less-${version}.tar.gz.sig less-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"

depends=("ncurses")
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "less-${version}.tar.gz.sig")

post_configure() {
    run_replace_in_file "s/#define HAVE_WCTYPE 1/\/* #undef HAVE_WCTYPE *\//" defines.h
    run touch stamp-h # prevent config.status from overwriting our changes
}
