#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(dirname "${0}")"

# shellcheck source=/dev/null
. "${SCRIPT_DIR}/shell_include.sh"

USE_FUSE2FS=0

if [ "$(id -u)" != 0 ]; then
    if [ -x "$FUSE2FS_PATH" ] && $FUSE2FS_PATH --help 2>&1 |grep fakeroot > /dev/null; then
        USE_FUSE2FS=1
    else
        set +e
        ${SUDO} -- "${SHELL}" -c "\"$0\" $* || exit 42"
        case $? in
            1)
                die "this script needs to run as root"
                ;;
            42)
                exit 1
                ;;
            *)
                exit 0
                ;;
        esac
    fi
else
    : "${SUDO_UID:=0}" "${SUDO_GID:=0}"
fi

# Prepend the toolchain qemu directory so we pick up QEMU from there
PATH="$SCRIPT_DIR/../Toolchain/Local/qemu/bin:$PATH"

INODE_SIZE=256
INODE_COUNT=$(($(inode_usage "$SERENITY_SOURCE_DIR/Base") + $(inode_usage Root)))
INODE_COUNT=$((INODE_COUNT + 2000))  # Some additional inodes for toolchain files, could probably also be calculated
DISK_SIZE_BYTES=$((($(disk_usage "$SERENITY_SOURCE_DIR/Base") + $(disk_usage Root) ) * 1024 * 1024))
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

if [ -n "$SERENITY_INODE_COUNT" ]; then
    if [ "$INODE_COUNT" -gt "$SERENITY_INODE_COUNT" ]; then
        die "SERENITY_INODE_COUNT is set to $SERENITY_INODE_COUNT but required inode count is roughly $INODE_COUNT"
    fi
    INODE_COUNT="$SERENITY_INODE_COUNT"
fi

nearest_power_of_2() {
    local n=$1
    local p=1
    while [ $p -lt "$n" ]; do
        p=$((p*2))
    done
    echo $p
}
if [ "$SERENITY_ARCH" = "aarch64" ] || [ "$SERENITY_BOOT_DRIVE" = "pci-sd" ]; then
    # SD cards must have a size that is a power of 2. The Aarch64 port loads from an SD card.
    DISK_SIZE_BYTES=$(nearest_power_of_2 "$DISK_SIZE_BYTES")
fi

USE_EXISTING=0

if [ -f _disk_image ]; then
    USE_EXISTING=1

    echo "checking existing image"
    result=0
    "$E2FSCK_PATH" -f -y _disk_image || result=$?
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
        newfs_ext2fs -D "${INODE_SIZE}" -n "${INODE_COUNT}" "/dev/r${VND}i" || die "could not create filesystem"
    else
        "${MKE2FS_PATH}" -q -I "${INODE_SIZE}" -N "${INODE_COUNT}" _disk_image || die "could not create filesystem"
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
        fi
        rm -rf mnt

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
    genext2fs -B 4096 -b $((DISK_SIZE_BYTES / 4096)) -N "${INODE_COUNT}" -d mnt _disk_image || die "try increasing image size (genext2fs -b)"
    # if using docker with shared mount, file is created as root, so make it writable for users
    chmod 0666 _disk_image
fi
