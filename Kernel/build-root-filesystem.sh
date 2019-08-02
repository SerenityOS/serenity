#!/bin/bash

set -e

# HACK: Get rid of old "qs" binaries still lying around from before it was renamed.
rm -f ../Userland/qs

die() {
    echo "die: $@"
    exit 1
}

if [ $(id -u) != 0 ]; then
    die "this script needs to run as root"
fi

echo -n "creating initial filesystem structure... "
mkdir -p mnt/{bin,etc,proc,tmp}
chmod 1777 mnt/tmp
echo "done"

echo -n "setting up device nodes... "
mkdir -p mnt/dev
mkdir -p mnt/dev/pts
mknod -m 666 mnt/dev/bxvga b 82 413
mknod mnt/dev/tty0 c 4 0
mknod mnt/dev/tty1 c 4 1
mknod mnt/dev/tty2 c 4 2
mknod mnt/dev/tty3 c 4 3
mknod mnt/dev/ttyS0 c 4 64
mknod mnt/dev/ttyS1 c 4 65
mknod mnt/dev/ttyS2 c 4 66
mknod mnt/dev/ttyS3 c 4 67
mknod -m 666 mnt/dev/random c 1 8
mknod -m 666 mnt/dev/null c 1 3
mknod -m 666 mnt/dev/zero c 1 5
mknod -m 666 mnt/dev/full c 1 7
mknod -m 666 mnt/dev/debuglog c 1 18
mknod mnt/dev/keyboard c 85 1
mknod mnt/dev/psaux c 10 1
mknod -m 666 mnt/dev/audio c 42 42
mknod -m 666 mnt/dev/ptmx c 5 2
mknod mnt/dev/hda b 3 0
mknod mnt/dev/hdb b 3 1
mknod mnt/dev/hdc b 4 0
mknod mnt/dev/hdd b 4 1
ln -s /proc/self/fd/0 mnt/dev/stdin
ln -s /proc/self/fd/1 mnt/dev/stdout
ln -s /proc/self/fd/2 mnt/dev/stderr
echo "done"

echo -n "installing base system... "
cp -R ../Base/* mnt/
cp -R ../Root/* mnt/
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
cp ../Applications/PaintBrush/PaintBrush mnt/bin/PaintBrush
cp ../Applications/QuickShow/QuickShow mnt/bin/QuickShow
cp ../Applications/Piano/Piano mnt/bin/Piano
cp ../Applications/SystemDialog/SystemDialog mnt/bin/SystemDialog
cp ../Demos/HelloWorld/HelloWorld mnt/bin/HelloWorld
cp ../Demos/HelloWorld2/HelloWorld2 mnt/bin/HelloWorld2
cp ../Demos/RetroFetch/RetroFetch mnt/bin/RetroFetch
cp ../Demos/WidgetGallery/WidgetGallery mnt/bin/WidgetGallery
cp ../Demos/Fire/Fire mnt/bin/Fire
cp ../DevTools/VisualBuilder/VisualBuilder mnt/bin/VisualBuilder
cp ../Games/Minesweeper/Minesweeper mnt/bin/Minesweeper
cp ../Games/Snake/Snake mnt/bin/Snake
cp ../Servers/LookupServer/LookupServer mnt/bin/LookupServer
cp ../Servers/SystemServer/SystemServer mnt/bin/SystemServer
cp ../Servers/WindowServer/WindowServer mnt/bin/WindowServer
cp ../Servers/AudioServer/AudioServer mnt/bin/AudioServer
cp ../Shell/Shell mnt/bin/Shell
cp ../Libraries/LibHTML/tho mnt/bin/tho
echo "done"

echo -n "installing shortcuts... "
ln -s Downloader mnt/bin/dl
ln -s FileManager mnt/bin/fm
ln -s HelloWorld mnt/bin/hw
ln -s HelloWorld2 mnt/bin/hw2
ln -s IRCClient mnt/bin/irc
ln -s Minesweeper mnt/bin/ms
ln -s Shell mnt/bin/sh
ln -s Snake mnt/bin/sn
ln -s Taskbar mnt/bin/tb
ln -s VisualBuilder mnt/bin/vb
ln -s WidgetGallery mnt/bin/wg
ln -s TextEditor mnt/bin/te
ln -s PaintBrush mnt/bin/pb
ln -s QuickShow mnt/bin/qs
ln -s Piano mnt/bin/pi
ln -s SystemDialog mnt/bin/sd
echo "done"

# Run local sync script, if it exists
if [ -f sync-local.sh ]; then
    sh sync-local.sh
fi
