#!/bin/sh

set -e

# HACK: Get rid of old "qs" binaries still lying around from before it was renamed.
rm -f ../Userland/qs

die() {
    echo "die: $*"
    exit 1
}

if [ "$(id -u)" != 0 ]; then
    die "this script needs to run as root"
fi

printf "creating initial filesystem structure... "
for dir in bin etc proc mnt tmp; do
    mkdir -p mnt/$dir
done
chmod 1777 mnt/tmp
echo "done"

printf "setting up device nodes... "
mkdir -p mnt/dev
mkdir -p mnt/dev/pts
mknod mnt/dev/fb0 b 29 0
chmod 666 mnt/dev/fb0
mknod mnt/dev/tty0 c 4 0
mknod mnt/dev/tty1 c 4 1
mknod mnt/dev/tty2 c 4 2
mknod mnt/dev/tty3 c 4 3
mknod mnt/dev/ttyS0 c 4 64
mknod mnt/dev/ttyS1 c 4 65
mknod mnt/dev/ttyS2 c 4 66
mknod mnt/dev/ttyS3 c 4 67
mknod mnt/dev/random c 1 8
mknod mnt/dev/null c 1 3
mknod mnt/dev/zero c 1 5
mknod mnt/dev/full c 1 7
mknod mnt/dev/debuglog c 1 18
# random, is failing (randomly) on fuse-ext2 on macos :)
chmod 666 mnt/dev/random || true 
chmod 666 mnt/dev/null
chmod 666 mnt/dev/zero
chmod 666 mnt/dev/full
chmod 666 mnt/dev/debuglog
mknod mnt/dev/keyboard c 85 1
mknod mnt/dev/psaux c 10 1
mknod mnt/dev/audio c 42 42
mknod mnt/dev/ptmx c 5 2
chmod 666 mnt/dev/audio
chmod 666 mnt/dev/ptmx
mknod mnt/dev/hda b 3 0
mknod mnt/dev/hdb b 3 1
mknod mnt/dev/hdc b 4 0
mknod mnt/dev/hdd b 4 1
ln -s /proc/self/fd/0 mnt/dev/stdin
ln -s /proc/self/fd/1 mnt/dev/stdout
ln -s /proc/self/fd/2 mnt/dev/stderr
echo "done"

printf "installing base system... "
cp -R ../Base/* mnt/
cp -R ../Root/* mnt/
cp kernel.map mnt/
echo "done"

printf "installing users... "
mkdir -p mnt/home/anon
mkdir -p mnt/home/nona
cp ../ReadMe.md mnt/home/anon/
chown -R 100:100 mnt/home/anon
chown -R 200:200 mnt/home/nona
echo "done"

printf "installing userland... "

if [ "$(uname)" != "Darwin" ]; then
find ../Userland/ -type f -executable -exec cp {} mnt/bin/ \;
else
find ../Userland/ -type f -perm +111 -exec cp {} mnt/bin/ \;
fi
chmod 4755 mnt/bin/su
echo "done"

printf "installing applications... "
cp ../Applications/About/About mnt/bin/About
cp ../Applications/FileManager/FileManager mnt/bin/FileManager
cp ../Applications/FontEditor/FontEditor mnt/bin/FontEditor
cp ../Applications/IRCClient/IRCClient mnt/bin/IRCClient
cp ../Applications/SystemMonitor/SystemMonitor mnt/bin/SystemMonitor
cp ../Applications/Taskbar/Taskbar mnt/bin/Taskbar
cp ../Applications/Terminal/Terminal mnt/bin/Terminal
cp ../Applications/TextEditor/TextEditor mnt/bin/TextEditor
cp ../Applications/HexEditor/HexEditor mnt/bin/HexEditor
cp ../Applications/PaintBrush/PaintBrush mnt/bin/PaintBrush
cp ../Applications/QuickShow/QuickShow mnt/bin/QuickShow
cp ../Applications/Piano/Piano mnt/bin/Piano
cp ../Applications/SystemDialog/SystemDialog mnt/bin/SystemDialog
cp ../Applications/ChanViewer/ChanViewer mnt/bin/ChanViewer
cp ../Applications/Calculator/Calculator mnt/bin/Calculator
cp ../Applications/SoundPlayer/SoundPlayer mnt/bin/SoundPlayer
cp ../Applications/DisplayProperties/DisplayProperties mnt/bin/DisplayProperties
cp ../Applications/Welcome/Welcome mnt/bin/Welcome
cp ../Applications/Help/Help mnt/bin/Help
cp ../Applications/Browser/Browser mnt/bin/Browser
cp ../Demos/HelloWorld/HelloWorld mnt/bin/HelloWorld
cp ../Demos/HelloWorld2/HelloWorld2 mnt/bin/HelloWorld2
cp ../Demos/WidgetGallery/WidgetGallery mnt/bin/WidgetGallery
cp ../Demos/Fire/Fire mnt/bin/Fire
cp ../DevTools/HackStudio/HackStudio mnt/bin/HackStudio
cp ../DevTools/VisualBuilder/VisualBuilder mnt/bin/VisualBuilder
cp ../DevTools/Inspector/Inspector mnt/bin/Inspector
cp ../DevTools/ProfileViewer/ProfileViewer mnt/bin/ProfileViewer
cp ../Games/Minesweeper/Minesweeper mnt/bin/Minesweeper
cp ../Games/Snake/Snake mnt/bin/Snake
cp ../Servers/LookupServer/LookupServer mnt/bin/LookupServer
cp ../Servers/SystemServer/SystemServer mnt/bin/SystemServer
cp ../Servers/WindowServer/WindowServer mnt/bin/WindowServer
cp ../Servers/AudioServer/AudioServer mnt/bin/AudioServer
cp ../Servers/TTYServer/TTYServer mnt/bin/TTYServer
cp ../Servers/TelnetServer/TelnetServer mnt/bin/TelnetServer
cp ../Servers/ProtocolServer/ProtocolServer mnt/bin/ProtocolServer
cp ../Shell/Shell mnt/bin/Shell
cp ../MenuApplets/Audio/Audio.MenuApplet mnt/bin/
cp ../MenuApplets/CPUGraph/CPUGraph.MenuApplet mnt/bin/
cp ../MenuApplets/Clock/Clock.MenuApplet mnt/bin/
echo "done"

printf "installing shortcuts... "
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
ln -s HexEditor mnt/bin/he
ln -s PaintBrush mnt/bin/pb
ln -s QuickShow mnt/bin/qs
ln -s Piano mnt/bin/pi
ln -s SystemDialog mnt/bin/sd
ln -s ChanViewer mnt/bin/cv
ln -s Calculator mnt/bin/calc
ln -s Inspector mnt/bin/ins
ln -s SoundPlayer mnt/bin/sp
ln -s Help mnt/bin/help
ln -s Browser mnt/bin/br
ln -s HackStudio mnt/bin/hs
ln -s SystemMonitor mnt/bin/sm
ln -s ProfileViewer mnt/bin/pv
echo "done"

mkdir -p mnt/boot/
cp kernel mnt/boot/

mkdir -p mnt/mod/
cp TestModule.o mnt/mod

# Run local sync script, if it exists
if [ -f sync-local.sh ]; then
    sh sync-local.sh
fi
