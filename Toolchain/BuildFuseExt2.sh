#!/usr/bin/env bash
set -e
# This file will need to be run in bash.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildFuseExt2.sh as root, parts of your Toolchain directory will become root-owned"

m4_prefix="$(brew --prefix m4)"
export PATH="${m4_prefix}/bin:$PATH"

die() {
    echo "die: $*"
    exit 1
}

if [[ "$OSTYPE" != "darwin"* ]]; then
    die "This script makes sense to be run only on macOS"
fi

SOURCE_DIR="$DIR/Tarballs/fuse-ext2"
PATCH_DIR="$DIR/Patches/fuse-ext2"
E2FSPROGS_PREFIX="$(brew --prefix e2fsprogs)"
MACFUSE_FRAMEWORK_DIR="/Library/Filesystems/macfuse.fs/Contents/Frameworks"

mkdir -p "$DIR"/Tarballs
pushd "$DIR"/Tarballs

if [ -d "$SOURCE_DIR" ]; then
    rm -rf "$SOURCE_DIR"
fi

git clone --depth 1 https://github.com/alperakcan/fuse-ext2.git "$SOURCE_DIR"

cd "$SOURCE_DIR"
for patch in "$PATCH_DIR"/*.patch; do
    patch -p1 < "$patch" > /dev/null
done

./autogen.sh
LDFLAGS="-L${E2FSPROGS_PREFIX}/lib"
if [ -d "${MACFUSE_FRAMEWORK_DIR}/MFMount.framework" ]; then
    LDFLAGS="${LDFLAGS} -F${MACFUSE_FRAMEWORK_DIR}"
fi
CFLAGS="-I/usr/local/include/osxfuse/ -I${E2FSPROGS_PREFIX}/include" LDFLAGS="${LDFLAGS}" ./configure
make \
    "fuse_ext2_wait_LINK=\$(OBJC) \$(OBJCFLAGS) \$(AM_OBJCFLAGS) \$(AM_LDFLAGS) \$(LDFLAGS) \$(fuse_ext2_wait_LDFLAGS) -o \$@" \
    "fuse_ext2_install_LINK=\$(OBJC) \$(OBJCFLAGS) \$(AM_OBJCFLAGS) \$(AM_LDFLAGS) \$(LDFLAGS) \$(fuse_ext2_install_LDFLAGS) -o \$@" \
    "OBJCLINK=\$(OBJC) \$(OBJCFLAGS) \$(AM_OBJCFLAGS) \$(AM_LDFLAGS) \$(LDFLAGS) -o \$@"
sudo make install
popd
