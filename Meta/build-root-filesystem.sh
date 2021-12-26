#!/bin/sh

set -e

wheel_gid=1
phys_gid=3
utmp_gid=5
window_uid=13
window_gid=13

CP="cp"

# cp on macOS and BSD systems do not support the -d option.
# gcp comes with coreutils, which is already a dependency.
OS="$(uname -s)"
if [ "$OS" = "Darwin" ] || echo "$OS" | grep -qe 'BSD$'; then
	CP="gcp"
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
$CP "$SERENITY_ROOT"/Toolchain/Local/i686/i686-pc-serenity/lib/libgcc_s.so mnt/usr/lib/
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
chmod 600 mnt/etc/shadow

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

printf "setting up device nodes folder... "
mkdir -p mnt/dev

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
cp -r "$SERENITY_ROOT"/Userland/Libraries/LibJS/Tests mnt/home/anon/js-tests
cp -r "$SERENITY_ROOT"/Userland/Libraries/LibWeb/Tests mnt/home/anon/web-tests
chmod 700 mnt/root
chmod 700 mnt/home/anon
chmod 700 mnt/home/nona
chown -R 0:0 mnt/root
chown -R 100:100 mnt/home/anon
chown -R 200:200 mnt/home/nona
echo "done"

printf "adding some desktop icons..."
ln -s /bin/Browser mnt/home/anon/Desktop/
ln -s /bin/TextEditor mnt/home/anon/Desktop/
ln -s /bin/Help mnt/home/anon/Desktop/
ln -s / "mnt/home/anon/Desktop/File system"

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
