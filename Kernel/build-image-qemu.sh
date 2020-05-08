#!/bin/sh

set -e

die() {
    echo "die: $*"
    exit 1
}

if [ "$(id -u)" != 0 ]; then
    die "this script needs to run as root (or with fakeroot)"
fi
if [ "$(uname -s)" = "Darwin" ]; then
    export PATH="/usr/local/opt/e2fsprogs/bin:$PATH"
    export PATH="/usr/local/opt/e2fsprogs/sbin:$PATH"
fi
echo "setting up disk image..."
qemu-img create _disk_image "${DISK_SIZE:-600}"m || die "could not create disk image"
chown "$SUDO_UID":"$SUDO_GID" _disk_image || die "could not adjust permissions on disk image"
echo "done"

use_genext2fs=0
if command -v genext2fs 1>/dev/null ; then
    use_genext2fs=1
else
    printf "creating new filesystem... "
    if [ "$(uname -s)" = "OpenBSD" ]; then
        VND=$(vnconfig _disk_image)
        (echo "e 0"; echo 83; echo n; echo 0; echo "*"; echo "quit") | fdisk -e "$VND"
        mkfs.ext2 -I 128 -F "/dev/${VND}i" || die "could not create filesystem"
    elif [ "$(uname -s)" = "FreeBSD" ]; then
        MD=$(mdconfig _disk_image)
        mke2fs -q -I 128 _disk_image || die "could not create filesystem"
    else
        if [ -x /sbin/mke2fs ]; then
            /sbin/mke2fs -q -I 128 _disk_image || die "could not create filesystem"
        else
            mke2fs -q -I 128 _disk_image || die "could not create filesystem"
        fi
    fi
    echo "done"

    printf "mounting filesystem... "
    mkdir -p mnt
    if [ "$(uname -s)" = "Darwin" ]; then
        fuse-ext2 _disk_image mnt -o rw+,allow_other,uid=501,gid=20 || die "could not mount filesystem"
    elif [ "$(uname -s)" = "OpenBSD" ]; then
        mount -t ext2fs "/dev/${VND}i" mnt/ || die "could not mount filesystem"
    elif [ "$(uname -s)" = "FreeBSD" ]; then
        fuse-ext2 -o rw+ "/dev/${MD}" mnt/ || die "could not mount filesystem"
    else
        mount _disk_image mnt/ || die "could not mount filesystem"
    fi
    echo "done"
fi

cleanup() {
    if [ -d mnt ]; then
        if [ $use_genext2fs = 0 ] ; then
            printf "unmounting filesystem... "
            umount mnt || ( sleep 1 && sync && umount mnt )
        fi
        rm -rf mnt
        if [ $use_genext2fs = 0 ] ; then
            if [ "$(uname -s)" = "OpenBSD" ]; then
                vnconfig -u "$VND"
            elif [ "$(uname -s)" = "FreeBSD" ]; then
                mdconfig -d -u "$MD"
            fi
        fi
        echo "done"
    fi
}
trap cleanup EXIT

./build-root-filesystem.sh

if [ $use_genext2fs = 1 ]; then
    # regenerate new image, since genext2fs is unable to reuse the previously written image.
    # genext2fs is very slow in generating big images, so I use a smaller image here. size can be updated
    # if it's not enough.
    # not using "-i 128" since it hangs. Serenity handles whatever default this uses instead.
    printf "generating filesystem... "
    genext2fs -b 250000 -d mnt _disk_image || die "try increasing image size (genext2fs -b)"
    echo "done"
    # if using docker with shared mount, file is created as root, so make it writable for users
    chmod 0666 _disk_image
fi
