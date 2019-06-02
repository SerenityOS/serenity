#!/bin/bash

set -e

die() {
    echo "die: $@"
    exit 1
}

if [ $(id -u) != 0 ]; then
    die "this script needs to run as root"
fi

echo "setting up disk image..."
if [ ! -f _disk_image ]; then
    echo "not found; creating a new one"
    dd if=/dev/zero of=_disk_image bs=1M count=100 || die "couldn't create disk image"
    parted -s _disk_image mklabel msdos mkpart primary ext2 32k 100% -a minimal set 1 boot on || die "couldn't partition disk image"
    chown 1000:1000 _disk_image || die "couldn't adjust permissions on disk image"
else
    echo "already exists, nothing to do"
fi
echo "done"

echo "checking for and removing old loopback devices..."
losetup -j _disk_image | cut -d : -f 1 | while read old_dev; do
    echo "removing $dev"
    losetup -d ${old_dev}
done
echo "done"

echo -n "creating loopback device... "
dev=$(losetup --find --partscan --show _disk_image)
if [ -z $dev ]; then
    die "couldn't mount loopback device"
fi
echo "loopback device is at ${dev}"

echo -n "destroying old filesystem... "
dd if=/dev/zero of=${dev}p1 bs=1M count=1 status=none
echo "done"

echo -n "creating new filesystem... "
mke2fs -q -I 128 ${dev}p1 || die "couldn't create filesystem"
echo "done"

echo -n "mounting loopback device... "
mkdir -p mnt
mount ${dev}p1 mnt/ || die "couldn't mount loopback device"
echo "done"

echo -n "creating initial filesystem structure... "
mkdir -p mnt/{boot,bin,etc,proc,tmp}
chmod 1777 mnt/tmp
echo "done"

echo "installing grub..."
mkdir -p mnt/boot/grub
cp grub.cfg mnt/boot/grub/grub.cfg
grub-install --boot-directory=mnt/boot --target=i386-pc --modules="ext2 part_msdos" ${dev}
echo "done"

echo -n "setting up device nodes... "
mkdir -p mnt/dev
mkdir -p mnt/dev/pts
mknod -m 666 mnt/dev/bxvga b 82 413
mknod mnt/dev/tty0 c 4 0
mknod mnt/dev/tty1 c 4 1
mknod mnt/dev/tty2 c 4 2
mknod mnt/dev/tty3 c 4 3
mknod mnt/dev/random c 1 8
mknod mnt/dev/null c 1 3
mknod mnt/dev/zero c 1 5
mknod mnt/dev/full c 1 7
mknod -m 666 mnt/dev/debuglog c 1 18
mknod mnt/dev/keyboard c 85 1
mknod mnt/dev/psaux c 10 1
mknod -m 666 mnt/dev/ptmx c 5 2
ln -s /proc/self/fd/0 mnt/dev/stdin
ln -s /proc/self/fd/1 mnt/dev/stdout
ln -s /proc/self/fd/2 mnt/dev/stderr
echo "done"

echo -n "installing base system... "
cp -R ../Base/* mnt/
cp -R ../Root/* mnt/
cp kernel mnt/boot
cp kernel.map mnt/
echo "done"

echo -n "installing users... "
mkdir -p mnt/home/anon
mkdir -p mnt/home/nona
cp ../ReadMe.md mnt/home/anon/
chown -R 100:100 mnt/home/anon
chown -R 200:200 mnt/home/nona
echo "done"

echo -n "installing userland... "
find ../Userland/ -type f -executable -exec cp {} mnt/bin/ \;
chmod 4755 mnt/bin/su
echo "done"

echo -n "installing applications... "
cp ../Applications/About/About mnt/bin/About
cp ../Applications/Downloader/Downloader mnt/bin/Downloader
cp ../Applications/FileManager/FileManager mnt/bin/FileManager
cp ../Applications/FontEditor/FontEditor mnt/bin/FontEditor
cp ../Applications/IRCClient/IRCClient mnt/bin/IRCClient
cp ../Applications/Launcher/Launcher mnt/bin/Launcher
cp ../Applications/ProcessManager/ProcessManager mnt/bin/ProcessManager
cp ../Applications/Taskbar/Taskbar mnt/bin/Taskbar
cp ../Applications/Terminal/Terminal mnt/bin/Terminal
cp ../Applications/TextEditor/TextEditor mnt/bin/TextEditor
cp ../Demos/HelloWorld/HelloWorld mnt/bin/HelloWorld
cp ../Demos/RetroFetch/RetroFetch mnt/bin/RetroFetch
cp ../Demos/WidgetGallery/WidgetGallery mnt/bin/WidgetGallery
cp ../DevTools/VisualBuilder/VisualBuilder mnt/bin/VisualBuilder
cp ../Games/Minesweeper/Minesweeper mnt/bin/Minesweeper
cp ../Games/Snake/Snake mnt/bin/Snake
cp ../Servers/LookupServer/LookupServer mnt/bin/LookupServer
cp ../Servers/SystemServer/SystemServer mnt/bin/SystemServer
cp ../Servers/WindowServer/WindowServer mnt/bin/WindowServer
cp ../Shell/Shell mnt/bin/Shell
echo "done"

echo -n "installing shortcuts... "
ln -s Downloader mnt/bin/dl
ln -s FileManager mnt/bin/fm
ln -s HelloWorld mnt/bin/hw
ln -s IRCClient mnt/bin/irc
ln -s Minesweeper mnt/bin/ms
ln -s Shell mnt/bin/sh
ln -s Snake mnt/bin/sn
ln -s Taskbar mnt/bin/tb
ln -s VisualBuilder mnt/bin/vb
ln -s WidgetGallery mnt/bin/wg
echo "done"

# Run local sync script, if it exists
if [ -f sync-local.sh ]; then
    sh sync-local.sh
fi

echo -n "unmounting filesystem... "
umount mnt || ( sleep 1 && sync && umount mnt )
echo "done"

echo -n "removing loopback device... "
losetup -d ${dev}
echo "done"
