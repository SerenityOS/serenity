#!/usr/bin/env -S bash ../.port_include.sh
port='rvvm'
version='0.5-git'
commit_hash=295bbf543b5db046abe20a0cd4935060f92864ff
archive_hash=2691a47163215b336b3cf497ba73a61f8f67084a0d95755f65215f8065fb8808
files="https://github.com/LekKit/RVVM/archive/$commit_hash.tar.gz rvvm.tar.gz $archive_hash"
auth_type='sha256'
workdir="RVVM-$commit_hash"
depends=('sdl12-compat')

build() {
    run make OS=SerenityOS USE_SDL=1 USE_NET=1 all lib
}

install() {
    run make OS=SerenityOS USE_SDL=1 USE_NET=1 DESTDIR="${DESTDIR}" install
}
