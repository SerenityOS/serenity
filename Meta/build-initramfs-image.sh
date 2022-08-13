#!/bin/sh

set -e

utmp_gid=5

die() {
    echo "die: $*"
    exit 1
}

[ -z "$SERENITY_SOURCE_DIR" ] && die "SERENITY_SOURCE_DIR is not set"
[ -d "$SERENITY_SOURCE_DIR/Base" ] || die "$SERENITY_SOURCE_DIR/Base doesn't exist"

printf "setting up /usr/lib folder in initramfs... "
mkdir -p InitRootFS/usr/lib
echo "done"

printf "setting up /bin folder in initramfs... "
mkdir -p InitRootFS/bin
echo "done"

printf "creating initial filesystem structure for initramfs... "
for dir in bin etc proc mnt tmp boot mod var/run usr/local; do
    mkdir -p InitRootFS/$dir
done
chmod 700 InitRootFS/boot
chmod 700 InitRootFS/mod
chmod 1777 InitRootFS/tmp
echo "done"

if rsync --chown 2>&1 | grep "missing argument" >/dev/null; then
    rsync -aH --chown=0:0 --inplace --update "$SERENITY_SOURCE_DIR"/Base/ InitRootFS/
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/libsystem.so InitRootFS/usr/lib/libsystem.so 
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/libc.so InitRootFS/usr/lib/libc.so 
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/libcore.so InitRootFS/usr/lib/libcore.so
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/libbuggiebox.so.serenity InitRootFS/usr/lib/libbuggiebox.so.serenity 
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/libbuggiebox.so InitRootFS/usr/lib/libbuggiebox.so 
    rsync -aH --chown=0:0 --inplace --update Root/bin/BuggieBox InitRootFS/bin/BuggieBox
    rsync -aH --chown=0:0 --inplace --update Root/usr/lib/Loader.so InitRootFS/usr/lib/Loader.so    
else
    rsync -aH --inplace --update "$SERENITY_SOURCE_DIR"/Base/ InitRootFS/
    rsync -aH --inplace --update Root/usr/lib/libsystem.so InitRootFS/usr/lib/libsystem.so 
    rsync -aH --inplace --update Root/usr/lib/libc.so InitRootFS/usr/lib/libc.so 
    rsync -aH --inplace --update Root/usr/lib/libcore.so InitRootFS/usr/lib/libcore.so
    rsync -aH --inplace --update Root/usr/lib/libbuggiebox.so.serenity InitRootFS/usr/lib/libbuggiebox.so.serenity 
    rsync -aH --inplace --update Root/usr/lib/libbuggiebox.so InitRootFS/usr/lib/libbuggiebox.so 
    rsync -aH --inplace --update Root/bin/BuggieBox InitRootFS/bin/BuggieBox
    rsync -aH --inplace --update Root/usr/lib/Loader.so InitRootFS/usr/lib/Loader.so
    chown -R 0:0 mnt/
fi

printf "setting up device nodes folder in initramfs... "
mkdir -p InitRootFS/dev
echo "done"

printf "setting up sysfs folder in initramfs... "
mkdir -p InitRootFS/sys
echo "done"

#############
printf "creating initial filesystem structure for initramfs... "
for dir in bin etc proc mnt tmp boot mod var/run usr/local; do
    mkdir -p InitRootFS/$dir
done
chmod 700 InitRootFS/boot
chmod 700 InitRootFS/mod
chmod 1777 InitRootFS/tmp
echo "done"
#############

printf "creating utmp file in initramfs... "
echo "{}" > InitRootFS/var/run/utmp
chown 0:$utmp_gid InitRootFS/var/run/utmp
chmod 664 InitRootFS/var/run/utmp


# If umask was 027 or similar when the repo was cloned,
# file permissions in Base/ are too restrictive. Restore
# the permissions needed in the image.
chmod -R g+rX,o+rX "$SERENITY_SOURCE_DIR"/Base/* InitRootFS/

echo "/bin/sh" > InitRootFS/etc/shells

printf "installing users... "
mkdir -p InitRootFS/root
mkdir -p InitRootFS/home/anon
mkdir -p InitRootFS/home/anon/Desktop
mkdir -p InitRootFS/home/anon/Downloads
mkdir -p InitRootFS/home/nona
chown -R 100:100 InitRootFS/home/anon
chown -R 200:200 InitRootFS/home/nona

printf "adding some desktop icons... "
ln -sf /bin/Browser InitRootFS/home/anon/Desktop/
ln -sf /bin/TextEditor InitRootFS/home/anon/Desktop/Text\ Editor
ln -sf /bin/Help InitRootFS/home/anon/Desktop/
ln -sf /home/anon InitRootFS/home/anon/Desktop/Home
chown -R 100:100 InitRootFS/home/anon/Desktop
echo "done"

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

printf "setting up initramfs archive... "
find InitRootFS/ -printf "%P\0" | cpio --directory=InitRootFS/ --quiet -oc0 > initramfs.cpio || die "Failed to create Initramfs cpio archive"
echo "done"
