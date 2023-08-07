#!/usr/bin/env -S bash ../.port_include.sh
port=citron
version=0.0.9.3
useconfigure=false
depends=(sparsehash libffi pcre)
commit_hash=d28b7d62bd61397e46152aa6e4ee59b115c0e2d7
archive_hash=0e31ab638c4fd1438f68fdf069336e2541eb4cfc5db2f55888f6175e0171a2ef
files=(
    "https://github.com/alimpfard/citron/archive/$commit_hash.tar.gz $commit_hash.tar.gz $archive_hash"
)
workdir="citron-$commit_hash"

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
