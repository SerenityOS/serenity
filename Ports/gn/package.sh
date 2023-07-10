#!/usr/bin/env -S bash ../.port_include.sh
port='gn'
workdir='gn'
version='2023.07.12'
repository='https://gn.googlesource.com/gn'
git_rev='fae280eabe5d31accc53100137459ece19a7a295'
useconfigure='true'
# FIXME: The files and auth_type are lies here. See #20004
files=('git')
auth_type='sha256'
depends=(
    'ninja'
    'python3'
)

fetch() {
    if [ ! -d ${workdir} ]; then
        git clone ${repository} ${workdir}
    fi
    run git checkout ${git_rev}
}

configure() {
    run python3 build/gen.py --platform='serenity' --allow-warnings
}

build() {
    run ninja -C out
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp out/gn "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp out/gn_unittests "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
