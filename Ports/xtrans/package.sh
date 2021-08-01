#!/usr/bin/env -S bash ../.port_include.sh
port=xtrans
version=1.4.0
useconfigure=true
files="https://www.x.org/releases/individual/lib/xtrans-${version}.tar.gz xtrans-${version}.tar.gz 48ed850ce772fef1b44ca23639b0a57e38884045ed2cbb18ab137ef33ec713f9"
depends="xproto xextproto"
auth_type="sha256"
