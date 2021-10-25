#!/usr/bin/env -S bash ../.port_include.sh
port=gperf
version=3.1
useconfigure="true"
depends=()
files="https://ftpmirror.gnu.org/gnu/gperf/gperf-${version}.tar.gz gperf-${version}.tar.gz
https://ftpmirror.gnu.org/gnu/gperf/gperf-${version}.tar.gz.sig gperf-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "gperf-${version}.tar.gz.sig")
configopts=("--prefix=/usr/local")
