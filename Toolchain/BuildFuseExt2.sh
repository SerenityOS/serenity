#!/usr/bin/env bash
set -e
# This file will need to be run in bash.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source=/dev/null
. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildFuseExt2.sh as root, parts of your Toolchain directory will become root-owned"

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
