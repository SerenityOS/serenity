#!/bin/bash

set -e

die() {
    echo "die: $@"
    exit 1
}

if [ $(id -u) != 0 ]; then
    die "this script needs to run as root"
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export SYSROOT=$DIR/mnt

echo -n "creating initial filesystem structure... "
mkdir -p $SYSROOT/{bin,etc,proc,tmp}
chmod 1777 $SYSROOT/tmp
echo "done"

echo -n "setting up device nodes... "
mkdir -p $SYSROOT/dev
mkdir -p $SYSROOT/dev/pts
mknod -m 666 $SYSROOT/dev/bxvga b 82 413
mknod $SYSROOT/dev/tty0 c 4 0
mknod $SYSROOT/dev/tty1 c 4 1
mknod $SYSROOT/dev/tty2 c 4 2
mknod $SYSROOT/dev/tty3 c 4 3
mknod $SYSROOT/dev/ttyS0 c 4 64
mknod $SYSROOT/dev/ttyS1 c 4 65
mknod $SYSROOT/dev/ttyS2 c 4 66
mknod $SYSROOT/dev/ttyS3 c 4 67
mknod -m 666 $SYSROOT/dev/random c 1 8
mknod -m 666 $SYSROOT/dev/null c 1 3
mknod -m 666 $SYSROOT/dev/zero c 1 5
mknod -m 666 $SYSROOT/dev/full c 1 7
mknod -m 666 $SYSROOT/dev/debuglog c 1 18
mknod $SYSROOT/dev/keyboard c 85 1
mknod $SYSROOT/dev/psaux c 10 1
mknod -m 666 $SYSROOT/dev/ptmx c 5 2
ln -s /proc/self/fd/0 $SYSROOT/dev/stdin
ln -s /proc/self/fd/1 $SYSROOT/dev/stdout
ln -s /proc/self/fd/2 $SYSROOT/dev/stderr
echo "done"

echo -n "installing base system... "
cp -R ../Base/* $SYSROOT/
cp -R ../Root/* $SYSROOT/
cp kernel.map $SYSROOT/
echo "done"

echo -n "installing users... "
mkdir -p $SYSROOT/home/anon
mkdir -p $SYSROOT/home/nona
cp ../ReadMe.md $SYSROOT/home/anon/
chown -R 100:100 $SYSROOT/home/anon
chown -R 200:200 $SYSROOT/home/nona
echo "done"

echo -n "installing userland... "
find ../Userland/ -type f -executable -exec cp {} $SYSROOT/bin/ \;
chmod 4755 $SYSROOT/bin/su
echo "done"

echo -n "installing applications... "

make -C ../Applications install

cp ../Demos/HelloWorld/HelloWorld $SYSROOT/bin/HelloWorld
cp ../Demos/RetroFetch/RetroFetch $SYSROOT/bin/RetroFetch
cp ../Demos/WidgetGallery/WidgetGallery $SYSROOT/bin/WidgetGallery
cp ../Demos/Fire/Fire $SYSROOT/bin/Fire
cp ../DevTools/VisualBuilder/VisualBuilder $SYSROOT/bin/VisualBuilder
cp ../Games/Minesweeper/Minesweeper $SYSROOT/bin/Minesweeper
cp ../Games/Snake/Snake $SYSROOT/bin/Snake
cp ../Servers/LookupServer/LookupServer $SYSROOT/bin/LookupServer
cp ../Servers/SystemServer/SystemServer $SYSROOT/bin/SystemServer
cp ../Servers/WindowServer/WindowServer $SYSROOT/bin/WindowServer
cp ../Shell/Shell $SYSROOT/bin/Shell
cp ../LibHTML/tho $SYSROOT/bin/tho
echo "done"

echo -n "installing shortcuts... "
ln -s Downloader $SYSROOT/bin/dl
ln -s FileManager $SYSROOT/bin/fm
ln -s HelloWorld $SYSROOT/bin/hw
ln -s IRCClient $SYSROOT/bin/irc
ln -s Minesweeper $SYSROOT/bin/ms
ln -s Shell $SYSROOT/bin/sh
ln -s Snake $SYSROOT/bin/sn
ln -s Taskbar $SYSROOT/bin/tb
ln -s VisualBuilder $SYSROOT/bin/vb
ln -s WidgetGallery $SYSROOT/bin/wg
ln -s TextEditor $SYSROOT/bin/te
ln -s PaintBrush $SYSROOT/bin/pb
ln -s QuickShow $SYSROOT/bin/qs
echo "done"

# Run local sync script, if it exists
if [ -f sync-local.sh ]; then
    sh sync-local.sh
fi
