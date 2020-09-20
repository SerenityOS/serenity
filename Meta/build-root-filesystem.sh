#!/bin/sh

set -e

wheel_gid=1
tty_gid=2
phys_gid=3
audio_gid=4
utmp_gid=5
window_uid=13
window_gid=13

CP="cp"

# cp on macOS does not support the -d option.
# gcp comes with coreutils, which is already a dependency.
if [ "$(uname -s)" = "Darwin" ]; then
	CP=gcp
fi

die() {
    echo "die: $*"
    exit 1
}

if [ "$(id -u)" != 0 ]; then
    die "this script needs to run as root"
fi

[ -z "$SERENITY_ROOT" ] && die "SERENITY_ROOT is not set"
[ -d "$SERENITY_ROOT/Base" ] || die "$SERENITY_ROOT/Base doesn't exist"

umask 0022

printf "installing base system... "
$CP -PdR "$SERENITY_ROOT"/Base/* mnt/
$CP -PdR Root/* mnt/
# If umask was 027 or similar when the repo was cloned,
# file permissions in Base/ are too restrictive. Restore
# the permissions needed in the image.
chmod -R g+rX,o+rX "$SERENITY_ROOT"/Base/* mnt/
chmod 400 mnt/res/kernel.map

chmod 660 mnt/etc/WindowServer/WindowServer.ini
chown $window_uid:$window_gid mnt/etc/WindowServer/WindowServer.ini
echo "/bin/sh" > mnt/etc/shells

chown 0:$wheel_gid mnt/bin/su
chown 0:$wheel_gid mnt/bin/passwd
chown 0:$phys_gid mnt/bin/keymap
chown 0:$phys_gid mnt/bin/shutdown
chown 0:$phys_gid mnt/bin/reboot
chown 0:0 mnt/boot/Kernel
chown 0:0 mnt/res/kernel.map
chmod 0400 mnt/res/kernel.map
chmod 0400 mnt/boot/Kernel
chmod 4750 mnt/bin/su
chmod 4755 mnt/bin/passwd
chmod 4755 mnt/bin/ping
chmod 4750 mnt/bin/reboot
chmod 4750 mnt/bin/shutdown
chmod 4750 mnt/bin/keymap
chown 0:$utmp_gid mnt/bin/utmpupdate
chmod 2755 mnt/bin/utmpupdate

echo "done"

printf "creating initial filesystem structure... "
for dir in bin etc proc mnt tmp boot mod var/run; do
    mkdir -p mnt/$dir
done
chmod 700 mnt/boot
chmod 700 mnt/mod
chmod 1777 mnt/tmp
echo "done"

printf "creating utmp file... "
touch mnt/var/run/utmp
chown 0:$utmp_gid mnt/var/run/utmp
chmod 664 mnt/var/run/utmp
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
# random, is failing (randomly) on fuse-ext2 on macos :)
chmod 666 mnt/dev/random || true
ln -s random mnt/dev/urandom
chmod 666 mnt/dev/null
chmod 666 mnt/dev/zero
chmod 666 mnt/dev/full
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

printf "writing version file... "
GIT_HASH=$( (git log --pretty=format:'%h' -n 1 | head -c 7) || true )
printf "[Version]\nMajor=1\nMinor=0\nGit=%s\n" "$GIT_HASH" > mnt/res/version.ini
echo "done"

printf "installing users... "
mkdir -p mnt/root
mkdir -p mnt/home/anon
mkdir -p mnt/home/anon/Desktop
mkdir -p mnt/home/anon/Downloads
mkdir -p mnt/home/nona
cp "$SERENITY_ROOT"/ReadMe.md mnt/home/anon/
cp -r "$SERENITY_ROOT"/Libraries/LibJS/Tests mnt/home/anon/js-tests
cp -r "$SERENITY_ROOT"/Libraries/LibWeb/Tests mnt/home/anon/web-tests
chmod 700 mnt/root
chmod 700 mnt/home/anon
chmod 700 mnt/home/nona
chown -R 0:0 mnt/root
chown -R 100:100 mnt/home/anon
chown -R 200:200 mnt/home/nona
echo "done"

printf "installing shortcuts... "
ln -s Shell mnt/bin/sh
ln -s test mnt/bin/[
echo "done"

printf "installing 'checksum' variants... "
ln -s checksum mnt/bin/md5sum
ln -s checksum mnt/bin/sha1sum
ln -s checksum mnt/bin/sha256sum
ln -s checksum mnt/bin/sha512sum
echo "done"

# Run local sync script, if it exists
if [ -f sync-local.sh ]; then
    sh sync-local.sh
fi
