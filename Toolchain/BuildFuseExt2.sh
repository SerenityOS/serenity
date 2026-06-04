#!/usr/bin/env bash
set -e
# This file will need to be run in bash.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildFuseExt2.sh as root, parts of your Toolchain directory will become root-owned"

die() {
    echo "die: $*"
    exit 1
}

SOURCE_DIR="$DIR/Tarballs/fuse-ext2"
PATCH_DIR="$DIR/Patches/fuse-ext2/macos"

EXTRA_CFLAGS=""
EXTRA_LDFLAGS=""

if [[ "$OSTYPE" == "darwin"* ]]; then
    if command -v brew &>/dev/null; then
        m4_prefix="$(brew --prefix m4 2>/dev/null)"
        [ -n "$m4_prefix" ] && export PATH="${m4_prefix}/bin:$PATH"
        E2FSPROGS_PREFIX="$(brew --prefix e2fsprogs 2>/dev/null)"
        EXTRA_CFLAGS="-I${E2FSPROGS_PREFIX}/include"
        EXTRA_LDFLAGS="-L${E2FSPROGS_PREFIX}/lib"
    elif command -v pkg-config &>/dev/null && pkg-config --exists ext2fs; then
        EXTRA_CFLAGS="$(pkg-config --cflags ext2fs)"
        EXTRA_LDFLAGS="$(pkg-config --libs ext2fs)"
    else
        die "Could not find e2fsprogs. Install it via Homebrew (brew install e2fsprogs) or ensure pkg-config can find ext2fs."
    fi
    # Support older osxfuse installs that put headers in /usr/local/include/osxfuse
    if [ -d "/usr/local/include/osxfuse" ]; then
        EXTRA_CFLAGS="-I/usr/local/include/osxfuse ${EXTRA_CFLAGS}"
    fi
    MACFUSE_FRAMEWORK_DIR="/Library/Filesystems/macfuse.fs/Contents/Frameworks"
    if [ -d "${MACFUSE_FRAMEWORK_DIR}/MFMount.framework" ]; then
        EXTRA_LDFLAGS="${EXTRA_LDFLAGS} -F${MACFUSE_FRAMEWORK_DIR}"
    fi
    MACOS_PATCHES=true
else
    if command -v pkg-config &>/dev/null && pkg-config --exists ext2fs; then
        EXTRA_CFLAGS="$(pkg-config --cflags ext2fs)"
        EXTRA_LDFLAGS="$(pkg-config --libs ext2fs)"
    else
        EXTRA_LDFLAGS="-lext2fs"
    fi
    MACOS_PATCHES=false
fi

mkdir -p "$DIR"/Tarballs
pushd "$DIR"/Tarballs

if [ -d "$SOURCE_DIR" ]; then
    rm -rf "$SOURCE_DIR"
fi

git clone --depth 1 https://github.com/alperakcan/fuse-ext2.git "$SOURCE_DIR"

cd "$SOURCE_DIR"
if [ "$MACOS_PATCHES" = true ]; then
    for patch in "$PATCH_DIR"/*.patch; do
        [ -f "$patch" ] || continue
        patch -p1 < "$patch" > /dev/null
    done
fi

./autogen.sh
CFLAGS="${EXTRA_CFLAGS}" LDFLAGS="${EXTRA_LDFLAGS}" ./configure
if [[ "$OSTYPE" == "darwin"* ]]; then
    make \
        "fuse_ext2_wait_LINK=\$(OBJC) \$(OBJCFLAGS) \$(AM_OBJCFLAGS) \$(AM_LDFLAGS) \$(LDFLAGS) \$(fuse_ext2_wait_LDFLAGS) -o \$@" \
        "fuse_ext2_install_LINK=\$(OBJC) \$(OBJCFLAGS) \$(AM_OBJCFLAGS) \$(AM_LDFLAGS) \$(LDFLAGS) \$(fuse_ext2_install_LDFLAGS) -o \$@" \
        "OBJCLINK=\$(OBJC) \$(OBJCFLAGS) \$(AM_OBJCFLAGS) \$(AM_LDFLAGS) \$(LDFLAGS) -o \$@"
else
    make
fi
sudo make install
popd
