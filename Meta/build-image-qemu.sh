#!/bin/sh

# Note: This is done before `set -e` to let `command` fail if needed
FUSE2FS_PATH=$(command -v fuse2fs)
RESIZE2FS_PATH=$(command -v resize2fs)

if [ -z "$FUSE2FS_PATH" ]; then
    FUSE2FS_PATH=/usr/sbin/fuse2fs
fi

if [ -z "$RESIZE2FS_PATH" ]; then
    RESIZE2FS_PATH=/usr/sbin/resize2fs
fi

set -e

die() {
    echo "die: $*"
    exit 1
}

USE_FUSE2FS=0

if [ "$(id -u)" != 0 ]; then
    if [ -x "$FUSE2FS_PATH" ] && $FUSE2FS_PATH --help 2>&1 |grep fakeroot > /dev/null; then
        USE_FUSE2FS=1
    else
        sudo -E -- "$0" "$@" || die "this script needs to run as root"
        exit 0
    fi
else
    : "${SUDO_UID:=0}" "${SUDO_GID:=0}"
fi

if [ "$(uname -s)" = "Darwin" ]; then
    export PATH="/usr/local/opt/e2fsprogs/bin:$PATH"
    export PATH="/usr/local/opt/e2fsprogs/sbin:$PATH"
    export PATH="/opt/homebrew/opt/e2fsprogs/bin:$PATH"
    export PATH="/opt/homebrew/opt/e2fsprogs/sbin:$PATH"

    E2FSCK="e2fsck"
else
    E2FSCK="/usr/sbin/e2fsck"

    if [ ! -f "$E2FSCK" ]; then
        E2FSCK=/sbin/e2fsck
    fi
fi

SCRIPT_DIR="$(dirname "${0}")"

# Prepend the toolchain qemu directory so we pick up QEMU from there
PATH="$SCRIPT_DIR/../Toolchain/Local/qemu/bin:$PATH"

# Also prepend the i686 toolchain directory because that's where most
# people will have their QEMU binaries if they built them before the
# directory was changed to Toolchain/Local/qemu.
PATH="$SCRIPT_DIR/../Toolchain/Local/i686/bin:$PATH"

# We depend on GNU coreutils du for the --apparent-size extension.
# GNU coreutils is a build dependency.
if command -v gdu > /dev/null 2>&1 && gdu --version | grep -q "GNU coreutils"; then
    GNUDU="gdu"
else
    GNUDU="du"
fi

disk_usage() {
    # shellcheck disable=SC2003
    expr "$(${GNUDU} -sk --apparent-size "$1" | cut -f1)"
}

inode_usage() {
    find "$1" | wc -l
}

INODE_SIZE=128
INODE_COUNT=$(($(inode_usage "$SERENITY_SOURCE_DIR/Base") + $(inode_usage Root)))
INODE_COUNT=$((INODE_COUNT + 2000))  # Some additional inodes for toolchain files, could probably also be calculated
DISK_SIZE_BYTES=$((($(disk_usage "$SERENITY_SOURCE_DIR/Base") + $(disk_usage Root)) * 1024))
DISK_SIZE_BYTES=$((DISK_SIZE_BYTES + (INODE_COUNT * INODE_SIZE)))

if [ -z "$SERENITY_DISK_SIZE_BYTES" ]; then
    # Try to use heuristics to guess a good disk size and inode count.
    # The disk must notably fit:
    #   * Data blocks (for both files and directories),
    #   * Indirect/doubly indirect/triply indirect blocks,
    #   * Inodes and block bitmaps for each block group,
    #   * Plenty of extra free space and free inodes.
    DISK_SIZE_BYTES=$((DISK_SIZE_BYTES * 2))
    INODE_COUNT=$((INODE_COUNT * 7))
else
    if [ "$DISK_SIZE_BYTES" -gt "$SERENITY_DISK_SIZE_BYTES" ]; then
        die "SERENITY_DISK_SIZE_BYTES is set to $SERENITY_DISK_SIZE_BYTES but required disk size is $DISK_SIZE_BYTES bytes"
    fi
    DISK_SIZE_BYTES="$SERENITY_DISK_SIZE_BYTES"
fi

USE_EXISTING=0

if [ -f _disk_image ]; then
    USE_EXISTING=1

    echo "checking existing image"
    result=0
    "$E2FSCK" -f -y _disk_image || result=$?
    if [ $result -ge 4 ]; then
        rm -f _disk_image
        USE_EXISTING=0
        echo "failed, not using existing image"
    else
        echo "done"
    fi
fi

if [ $USE_EXISTING -eq 1 ];  then
    OLD_DISK_SIZE_BYTES=$(wc -c < _disk_image)
    if [ "$DISK_SIZE_BYTES" -gt "$OLD_DISK_SIZE_BYTES" ]; then
        echo "resizing disk image..."
        qemu-img resize -f raw _disk_image "$DISK_SIZE_BYTES" || die "could not resize disk image"
        if ! "$RESIZE2FS_PATH" _disk_image; then
            rm -f _disk_image
            USE_EXISTING=0
            echo "failed, not using existing image"
        fi
        echo "done"
    fi
fi

if [ $USE_EXISTING -ne 1 ]; then
    printf "setting up disk image... "
    qemu-img create -q -f raw _disk_image "$DISK_SIZE_BYTES" || die "could not create disk image"
    chown "$SUDO_UID":"$SUDO_GID" _disk_image || die "could not adjust permissions on disk image"
    echo "done"

    printf "creating new filesystem... "
    if [ "$(uname -s)" = "OpenBSD" ]; then
        VND=$(vnconfig _disk_image)
        (echo "e 0"; echo 83; echo n; echo 0; echo "*"; echo "quit") | fdisk -e "$VND"
        newfs_ext2fs -D $INODE_SIZE -n $INODE_COUNT "/dev/r${VND}i" || die "could not create filesystem"
    else
        if [ -x /sbin/mke2fs ]; then
            /sbin/mke2fs -q -I $INODE_SIZE -N $INODE_COUNT _disk_image || die "could not create filesystem"
        else
            mke2fs -q -I $INODE_SIZE -N $INODE_COUNT _disk_image || die "could not create filesystem"
        fi
    fi
    echo "done"
fi

printf "mounting filesystem... "
mkdir -p mnt
use_genext2fs=0
if [ $USE_FUSE2FS -eq 1 ]; then
    mount_cmd="$FUSE2FS_PATH _disk_image mnt/ -o fakeroot,rw"
elif [ "$(uname -s)" = "Darwin" ]; then
    mount_cmd="fuse-ext2 _disk_image mnt -o rw+,allow_other,uid=501,gid=20"
elif [ "$(uname -s)" = "OpenBSD" ]; then
    VND=$(vnconfig _disk_image)
    mount_cmd="mount -t ext2fs "/dev/${VND}i" mnt/"
elif [ "$(uname -s)" = "FreeBSD" ]; then
    MD=$(mdconfig _disk_image)
    mount_cmd="fuse-ext2 -o rw+,direct_io "/dev/${MD}" mnt/"
else
    mount_cmd="mount _disk_image mnt/"
fi
if ! eval "$mount_cmd"; then
    if command -v genext2fs 1>/dev/null ; then
        echo "mount failed but genext2fs exists, use it instead"
        use_genext2fs=1
    else
        die "could not mount filesystem and genext2fs is missing"
    fi
else
    echo "done"
fi

cleanup() {
    if [ -d mnt ]; then
        if [ $use_genext2fs = 0 ] ; then
            printf "unmounting filesystem... "
            if [ $USE_FUSE2FS -eq 1 ]; then
                fusermount -u mnt || (sleep 1 && sync && fusermount -u mnt)
            else
                umount mnt || ( sleep 1 && sync && umount mnt )
            fi
            rmdir mnt
        else
            rm -rf mnt
        fi

        if [ "$(uname -s)" = "OpenBSD" ]; then
            vnconfig -u "$VND"
        elif [ "$(uname -s)" = "FreeBSD" ]; then
            mdconfig -d -u "$MD"
        fi
        echo "done"
    fi
}
trap cleanup EXIT

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
"$script_path/build-root-filesystem.sh"

if [ $use_genext2fs = 1 ]; then
    # regenerate new image, since genext2fs is unable to reuse the previously written image.
    # genext2fs is very slow in generating big images, so I use a smaller image here. size can be updated
    # if it's not enough.
    # not using "-I $INODE_SIZE" since it hangs. Serenity handles whatever default this uses instead.
    genext2fs -B 4096 -b $((DISK_SIZE_BYTES / 4096)) -N $INODE_COUNT -d mnt _disk_image || die "try increasing image size (genext2fs -b)"
    # if using docker with shared mount, file is created as root, so make it writable for users
    chmod 0666 _disk_image
fi
