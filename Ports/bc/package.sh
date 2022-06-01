#!/usr/bin/env -S bash ../.port_include.sh
port='bc'
version='5.2.5'
files="https://github.com/gavinhoward/bc/releases/download/${version}/bc-${version}.tar.xz bc-${version}.tar.xz
https://github.com/gavinhoward/bc/releases/download/${version}/bc-${version}.tar.xz.sig bc-${version}.tar.xz.sig"
useconfigure='true'
configscript='configure.sh'
auth_type="sig"
auth_import_key="E2A30324A4465A4D5882692EC08038BDF280D33E"
auth_opts=("bc-${version}.tar.xz.sig")
configopts=("--prefix=/usr/local" "--disable-nls")

export CFLAGS="-O3 -flto"

configure() {
    run env HOSTCC=gcc ./"$configscript" "${configopts[@]}"
}
