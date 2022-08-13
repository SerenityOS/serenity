#!/bin/sh

set -e

die() {
    echo "die: $*"
    exit 1
}

[ -z "$SERENITY_SOURCE_DIR" ] && die "SERENITY_SOURCE_DIR is not set"
[ -d "$SERENITY_SOURCE_DIR/Base" ] || die "$SERENITY_SOURCE_DIR/Base doesn't exist"

printf "creating initial filesystem structure for initramfs... "
for dir in dev sys bin etc proc mnt tmp boot mod var/run usr/local usr/lib; do
    mkdir -p InitRootFS/$dir
done

if rsync --chown 2>&1 | grep "missing argument" >/dev/null; then
    rsync -aH --chown=0:0 --inplace --update "$SERENITY_SOURCE_DIR"/Base/ InitRootFS/
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/libsystem.so InitRootFS/usr/lib/libsystem.so 
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/libc.so InitRootFS/usr/lib/libc.so 
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/libcore.so.serenity InitRootFS/usr/lib/libcore.so.serenity
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/libcore.so InitRootFS/usr/lib/libcore.so
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/libbuggiebox.so.serenity InitRootFS/usr/lib/libbuggiebox.so.serenity 
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/libbuggiebox.so InitRootFS/usr/lib/libbuggiebox.so 
    rsync -aH --chown=0:0 --inplace --update Root/bin/BuggieBox InitRootFS/bin/BuggieBox
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/Loader.so InitRootFS/usr/lib/Loader.so    
else
    rsync -aH --inplace --update "$SERENITY_SOURCE_DIR"/Base/ InitRootFS/
    rsync -aH --inplace --update Root/usr/lib/libsystem.so InitRootFS/usr/lib/libsystem.so 
    rsync -aH --inplace --update Root/usr/lib/libc.so InitRootFS/usr/lib/libc.so 
    rsync -aH --inplace --update Root/usr/lib/libcore.so.serenity InitRootFS/usr/lib/libcore.so.serenity
    rsync -aH --inplace --update Root/usr/lib/libcore.so InitRootFS/usr/lib/libcore.so
    rsync -aH --inplace --update Root/usr/lib/libbuggiebox.so.serenity InitRootFS/usr/lib/libbuggiebox.so.serenity 
    rsync -aH --inplace --update Root/usr/lib/libbuggiebox.so InitRootFS/usr/lib/libbuggiebox.so 
    rsync -aH --inplace --update Root/bin/BuggieBox InitRootFS/bin/BuggieBox
    rsync -aH --inplace --update Root/usr/lib/Loader.so InitRootFS/usr/lib/Loader.so
fi

chmod 700 InitRootFS/boot
chmod 700 InitRootFS/mod
chmod 1777 InitRootFS/tmp
echo "done"

printf "creating utmp file in initramfs... "
echo "{}" > InitRootFS/var/run/utmp
chmod 664 InitRootFS/var/run/utmp

# If umask was 027 or similar when the repo was cloned,
# file permissions in Base/ are too restrictive. Restore
# the permissions needed in the image.
chmod -R g+rX,o+rX "$SERENITY_SOURCE_DIR"/Base/* InitRootFS/

echo "/bin/sh" > InitRootFS/etc/shells

printf "installing users... "
mkdir -p InitRootFS/root

printf "installing shortcuts... "
ln -sf /bin/BuggieBox InitRootFS/bin/uname
ln -sf /bin/BuggieBox InitRootFS/bin/env
ln -sf /bin/BuggieBox InitRootFS/bin/lsblk
ln -sf /bin/BuggieBox InitRootFS/bin/file
ln -sf /bin/BuggieBox InitRootFS/bin/df
ln -sf /bin/BuggieBox InitRootFS/bin/mount
ln -sf /bin/BuggieBox InitRootFS/bin/umount
ln -sf /bin/BuggieBox InitRootFS/bin/mkdir
ln -sf /bin/BuggieBox InitRootFS/bin/rmdir
ln -sf /bin/BuggieBox InitRootFS/bin/rm
ln -sf /bin/BuggieBox InitRootFS/bin/chown
ln -sf /bin/BuggieBox InitRootFS/bin/chmod
ln -sf /bin/BuggieBox InitRootFS/bin/cp
ln -sf /bin/BuggieBox InitRootFS/bin/ln
ln -sf /bin/BuggieBox InitRootFS/bin/ls
ln -sf /bin/BuggieBox InitRootFS/bin/mv
ln -sf /bin/BuggieBox InitRootFS/bin/cat
ln -sf /bin/BuggieBox InitRootFS/bin/md5sum
ln -sf /bin/BuggieBox InitRootFS/bin/sha1sum
ln -sf /bin/BuggieBox InitRootFS/bin/sha256sum
ln -sf /bin/BuggieBox InitRootFS/bin/sha512sum
ln -sf /bin/BuggieBox InitRootFS/bin/sh
ln -sf /bin/BuggieBox InitRootFS/bin/uniq
ln -sf /bin/BuggieBox InitRootFS/bin/id
ln -sf /bin/BuggieBox InitRootFS/bin/tail
ln -sf /bin/BuggieBox InitRootFS/bin/find
ln -sf /bin/BuggieBox InitRootFS/bin/less
ln -sf /bin/BuggieBox InitRootFS/bin/mknod
ln -sf /bin/BuggieBox InitRootFS/bin/ps
ln -sf /bin/BuggieBox InitRootFS/bin/Shell
ln -sf /bin/BuggieBox InitRootFS/bin/init
ln -sf /bin/BuggieBox InitRootFS/bin/test
ln -sf /bin/BuggieBox InitRootFS/bin/clear
ln -sf /bin/BuggieBox InitRootFS/bin/dmesg
ln -sf /bin/BuggieBox InitRootFS/bin/errno
ln -sf /bin/BuggieBox InitRootFS/bin/base64
ln -sf /bin/BuggieBox InitRootFS/bin/unveil
ln -sf /bin/BuggieBox InitRootFS/bin/pledge
ln -sf /bin/BuggieBox InitRootFS/bin/whoami
ln -sf /bin/BuggieBox InitRootFS/bin/touch
ln -sf /bin/BuggieBox InitRootFS/bin/top
ln -sf /bin/BuggieBox InitRootFS/bin/timezone
ln -sf /bin/BuggieBox InitRootFS/bin/tac
ln -sf /bin/BuggieBox InitRootFS/bin/sysctl
ln -sf /bin/BuggieBox InitRootFS/bin/sync
ln -sf /bin/BuggieBox InitRootFS/bin/stat
ln -sf /bin/BuggieBox InitRootFS/bin/sort
ln -sf /bin/BuggieBox InitRootFS/bin/shuf
ln -sf /bin/BuggieBox InitRootFS/bin/mktemp
ln -sf /bin/BuggieBox InitRootFS/bin/lspci
ln -sf /bin/BuggieBox InitRootFS/bin/lsusb
ln -sf /bin/BuggieBox InitRootFS/bin/lsirq
ln -sf /bin/BuggieBox InitRootFS/bin/lscpu
ln -sf /bin/BuggieBox InitRootFS/bin/ldd
ln -sf /bin/BuggieBox InitRootFS/bin/readelf
ln -sf test InitRootFS/bin/[
echo "done"

CP="cp"

# cp on macOS and BSD systems do not support the --preserve= option.
# gcp comes with coreutils, which is already a dependency.
OS="$(uname -s)"
if [ "$OS" = "Darwin" ] || echo "$OS" | grep -qe 'BSD$'; then
	CP="gcp"
fi

if [ "$SERENITY_ARCH" != "aarch64" ]; then
    $CP --preserve=timestamps "$SERENITY_SOURCE_DIR"/Toolchain/Local/"$SERENITY_ARCH"/"$SERENITY_ARCH"-pc-serenity/lib/libgcc_s.so InitRootFS/usr/lib
fi

echo "done setting up initramfs directory"
