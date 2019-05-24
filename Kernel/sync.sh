#!/bin/bash

if [ "$1" = "-f" ]; then
    rm -vf _fs_contents
fi

if [ $(id -u) != 0 ]; then
    echo "This needs to be run as root"
    exit 1
fi

rm -vf _fs_contents.lock

# If target filesystem image doesn't exist, create it.
if [ ! -f _fs_contents ]; then
    dd if=/dev/zero of=_fs_contents bs=1M count=512
fi

mke2fs -F -I 128 _fs_contents

chown 1000:1000 _fs_contents
mkdir -vp mnt
mount -o loop _fs_contents mnt/
mkdir -vp mnt/bin
mkdir -vp mnt/etc
mkdir -vp mnt/proc
mkdir -vp mnt/tmp
chmod 1777 mnt/tmp
mkdir -vp mnt/dev
mkdir -vp mnt/dev/pts
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
cp -vR ../Base/* mnt/
cp -vR ../Root/* mnt/
mkdir -vp mnt/home/anon
mkdir -vp mnt/home/nona
cp ../ReadMe.md mnt/home/anon/
chown -vR 100:100 mnt/home/anon
chown -vR 200:200 mnt/home/nona
find ../Userland/ -type f -executable -exec cp -v {} mnt/bin/ \;
chmod 4755 mnt/bin/su
cp -v ../Applications/Terminal/Terminal mnt/bin/Terminal
cp -v ../Applications/FontEditor/FontEditor mnt/bin/FontEditor
cp -v ../Applications/Launcher/Launcher mnt/bin/Launcher
cp -v ../Applications/FileManager/FileManager mnt/bin/FileManager
cp -v ../Applications/ProcessManager/ProcessManager mnt/bin/ProcessManager
cp -v ../Applications/About/About mnt/bin/About
cp -v ../Applications/TextEditor/TextEditor mnt/bin/TextEditor
cp -v ../Applications/IRCClient/IRCClient mnt/bin/IRCClient
ln -s IRCClient mnt/bin/irc
ln -s FileManager mnt/bin/fm
cp -v ../Servers/LookupServer/LookupServer mnt/bin/LookupServer
cp -v ../Servers/WindowServer/WindowServer mnt/bin/WindowServer
cp -v ../Applications/Taskbar/Taskbar mnt/bin/Taskbar
ln -s Taskbar mnt/bin/tb
cp -v ../Applications/Downloader/Downloader mnt/bin/Downloader
ln -s Downloader mnt/bin/dl
cp -v ../DevTools/VisualBuilder/VisualBuilder mnt/bin/VisualBuilder
ln -s VisualBuilder mnt/bin/vb
cp -v ../Games/Minesweeper/Minesweeper mnt/bin/Minesweeper
ln -s Minesweeper mnt/bin/ms
cp -v ../Games/Snake/Snake mnt/bin/Snake
ln -s Snake mnt/bin/sn
cp -v ../Shell/Shell mnt/bin/Shell
ln -s Shell mnt/bin/sh
cp -v kernel.map mnt/
cp -v ../Demos/HelloWorld/HelloWorld mnt/bin/HelloWorld
ln -s HelloWorld mnt/bin/hw
cp -v ../Demos/RetroFetch/RetroFetch mnt/bin/RetroFetch
cp -v ../Demos/WidgetGallery/WidgetGallery mnt/bin/WidgetGallery
ln -s WidgetGallery mnt/bin/wg

# Run local sync script, if it exists
if [ -f sync-local.sh ]; then
    sh sync-local.sh
fi

umount mnt || ( sleep 0.5 && sync && umount mnt )
