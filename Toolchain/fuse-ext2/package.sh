#!/bin/bash ../.toolchain_include.sh

package=fuse-ext2
version=git-master
useconfigure=true
installopts="install DESTDIR=$PREFIX"
filename="master.zip"
files="https://github.com/alperakcan/fuse-ext2/archive/$filename $filename"

die() {
    echo "+ die '$@'"
    exit 1
}

if [[ "$OSTYPE" != "darwin"* ]]; then
    die "This script makes sense to be run only on macOS"
fi

export PATH="/usr/local/opt/m4/bin:$PATH"
export CFLAGS="$CFLAGS -I/usr/local/include/osxfuse/ -I/$(brew --prefix e2fsprogs)/include"
export LDFLAGS="$LDFLAGS -L$(brew --prefix e2fsprogs)/lib"

postfetch() {
    run_sourcedir ./autogen.sh
}
