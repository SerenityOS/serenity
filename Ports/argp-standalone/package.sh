#!/usr/bin/env -S bash ../.port_include.sh
port='argp-standalone'
version='1.0.2'
useconfigure='true'
files="https://github.com/tom42/argp-standalone/archive/refs/tags/v1.0.2.zip argp.zip 75b624a5e9e89d5e7527400a36c9b434e3f7eeaf3e8c19baf018ea4be3e250f9"
auth_type='sha256'

configure() {
    run cmake "${configopts[@]}" .
    #run cmake "${configopts[@]}" src/
}

install() {
	cp -r "${port}-${version}"/include/argp-standalone/argp.h "${SERENITY_INSTALL_ROOT}/usr/include/argp.h"
    cp -r "${port}-${version}"/src/libargp-standalone.so "${SERENITY_INSTALL_ROOT}/usr/lib/libargp.so"
}
