#!/usr/bin/env -S bash ../.port_include.sh
port=citron
version=0.0.9.3
useconfigure=false
depends=(sparsehash libffi)
files="https://github.com/alimpfard/citron/archive/refs/heads/master.tar.gz citron.tar.gz d2a607bb662e2d862503a5eeb034ae65ce0bb9da5ad48c289d5325f2d0e14167"
auth_type=sha256
workdir=citron-master

pre_install() {
    pushd "$workdir"
    if [ -d Library/.git ]; then
        git -C Library pull
    else
        rm -fr Library
        git clone http://github.com/alimpfard/citron_standard_library Library
    fi
    popd
}
