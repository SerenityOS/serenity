#!/usr/bin/env -S bash ../.port_include.sh
port='citron'
version='0.0.9.3'
useconfigure='false'
depends=(
    'libffi'
    'pcre'
    'sparsehash'
)
commit_hash='c0bafa246bb2282125858da54e084c8085288d5c'
archive_hash='f4d77cc8f70a59a4d495fbf0cfc8a9654742817f87c50f5b0e46eef54b5413f7'
files=(
    "https://github.com/alimpfard/citron/archive/$commit_hash.tar.gz#$archive_hash"
)
workdir="citron-$commit_hash"
export enable_boehm_gc=false
export enable_inject=false
export use_libbsd=false
export use_openmp=false

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
