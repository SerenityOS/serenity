#!/bin/sh
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export PATH="/usr/local/opt/m4/bin:$PATH"

die() {
    echo "die: $*"
    exit 1
}

if [[ "$OSTYPE" != "darwin"* ]]; then
    die "This script makes sense to be run only on macOS"
fi

mkdir -p "$DIR"/Tarballs
pushd "$DIR"/Tarballs

if [ ! -d fuse-ext2 ]; then
    git clone https://github.com/alperakcan/fuse-ext2.git	
fi

cd fuse-ext2
./autogen.sh
CFLAGS="-I/usr/local/include/osxfuse/ -I/$(brew --prefix e2fsprogs)/include" LDFLAGS="-L$(brew --prefix e2fsprogs)/lib" ./configure
make
sudo make install
popd
