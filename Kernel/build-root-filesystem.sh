#!/bin/sh

set -e

wheel_gid=1
tty_gid=2
phys_gid=3
audio_gid=4
window_uid=13
window_gid=13

die() {
    echo "die: $*"
    exit 1
}

if [ "$(id -u)" != 0 ]; then
    die "this script needs to run as root"
fi

umask 0022

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
chmod 660 mnt/dev/fb0
chown 0:$phys_gid mnt/dev/fb0
mknod mnt/dev/tty0 c 4 0
mknod mnt/dev/tty1 c 4 1
mknod mnt/dev/tty2 c 4 2
mknod mnt/dev/tty3 c 4 3
mknod mnt/dev/ttyS0 c 4 64
mknod mnt/dev/ttyS1 c 4 65
mknod mnt/dev/ttyS2 c 4 66
mknod mnt/dev/ttyS3 c 4 67
for tty in 0 1 2 3 S0 S1 S2 S3; do
    chmod 620 mnt/dev/tty$tty
    chown 0:$tty_gid mnt/dev/tty$tty
done
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
chmod 440 mnt/dev/keyboard
chown 0:$phys_gid mnt/dev/keyboard
mknod mnt/dev/mouse c 10 1
chmod 440 mnt/dev/mouse
chown 0:$phys_gid mnt/dev/mouse
mknod mnt/dev/audio c 42 42
chmod 220 mnt/dev/audio
chown 0:$audio_gid mnt/dev/audio
mknod mnt/dev/ptmx c 5 2
chmod 666 mnt/dev/ptmx
mknod mnt/dev/hda b 3 0
mknod mnt/dev/hdb b 3 1
mknod mnt/dev/hdc b 4 0
mknod mnt/dev/hdd b 4 1
for hd in a b c d; do
    chmod 600 mnt/dev/hd$hd
done

ln -s /proc/self/fd/0 mnt/dev/stdin
ln -s /proc/self/fd/1 mnt/dev/stdout
ln -s /proc/self/fd/2 mnt/dev/stderr
echo "done"

printf "installing base system... "
cp -R ../Base/* mnt/
cp -R ../Root/* mnt/
cp kernel.map mnt/res/
chmod 400 mnt/res/kernel.map

chmod 660 mnt/etc/WindowServer/WindowServer.ini
chown $window_uid:$window_gid mnt/etc/WindowServer/WindowServer.ini

echo "done"

printf "installing users... "
mkdir -p mnt/home/anon
mkdir -p mnt/home/nona
cp ../ReadMe.md mnt/home/anon/
chmod 700 mnt/home/anon
chmod 700 mnt/home/nona
chown -R 100:100 mnt/home/anon
chown -R 200:200 mnt/home/nona
echo "done"

printf "installing userland... "

if [ "$(uname -s)" = "Darwin" ]; then
    find ../Userland/ -type f -perm +111 -exec cp {} mnt/bin/ \;
elif [ "$(uname -s)" = "OpenBSD" ]; then
    find ../Userland/ -type f -perm -555 -exec cp {} mnt/bin/ \;
else
    find ../Userland/ -type f -executable -exec cp {} mnt/bin/ \;
fi
chown 0:$wheel_gid mnt/bin/su
chmod 4750 mnt/bin/su
chmod 4755 mnt/bin/ping
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
cp ../Applications/SystemMenu/SystemMenu mnt/bin/SystemMenu
cp ../Applications/ChanViewer/ChanViewer mnt/bin/ChanViewer
cp ../Applications/Calculator/Calculator mnt/bin/Calculator
cp ../Applications/SoundPlayer/SoundPlayer mnt/bin/SoundPlayer
cp ../Applications/DisplayProperties/DisplayProperties mnt/bin/DisplayProperties
cp ../Applications/Welcome/Welcome mnt/bin/Welcome
cp ../Applications/Help/Help mnt/bin/Help
cp ../Applications/Browser/Browser mnt/bin/Browser
cp ../Demos/HelloWorld/HelloWorld mnt/bin/HelloWorld
cp ../Demos/WidgetGallery/WidgetGallery mnt/bin/WidgetGallery
cp ../Demos/Fire/Fire mnt/bin/Fire
cp ../Demos/DynamicLink/LinkDemo/LinkDemo mnt/bin/LinkDemo
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
cp ../Servers/NotificationServer/NotificationServer mnt/bin/NotificationServer
cp ../Servers/WebServer/WebServer mnt/bin/WebServer
cp ../Shell/Shell mnt/bin/Shell
cp ../MenuApplets/Audio/Audio.MenuApplet mnt/bin/
cp ../MenuApplets/CPUGraph/CPUGraph.MenuApplet mnt/bin/
cp ../MenuApplets/Clock/Clock.MenuApplet mnt/bin/
cp ../MenuApplets/UserName/UserName.MenuApplet mnt/bin
echo "done"

printf "installing dynamic libraries... "
cp ../Demos/DynamicLink/LinkLib/libDynamicLib.so mnt/usr/lib
echo "done"

printf "installing shortcuts... "
ln -s FileManager mnt/bin/fm
ln -s HelloWorld mnt/bin/hw
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
ln -s WebServer mnt/bin/ws
echo "done"

mkdir -p mnt/boot/
chmod 700 mnt/boot/
cp kernel mnt/boot/
chmod 600 mnt/boot/kernel

mkdir -p mnt/mod/
chmod 700 mnt/mod/
cp TestModule.kernel.o mnt/mod/TestModule.o
chmod 600 mnt/mod/*.o

# Run local sync script, if it exists
if [ -f sync-local.sh ]; then
    sh sync-local.sh
fi
