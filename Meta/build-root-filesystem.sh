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

[ -z "$SERENITY_SOURCE_DIR" ] && die "SERENITY_SOURCE_DIR is not set"
[ -d "$SERENITY_SOURCE_DIR/Base" ] || die "$SERENITY_SOURCE_DIR/Base doesn't exist"

umask 0022

printf "installing base system... "
if ! command -v rsync >/dev/null; then
    die "Please install rsync."
fi

if rsync --chown 2>&1 | grep "missing argument" >/dev/null; then
    rsync -aH --chown=0:0 --inplace "$SERENITY_SOURCE_DIR"/Base/ mnt/
    rsync -aH --chown=0:0 --inplace Root/ mnt/
else
    rsync -aH --inplace "$SERENITY_SOURCE_DIR"/Base/ mnt/
    rsync -aH --inplace Root/ mnt/
    chown -R 0:0 mnt/
fi

SERENITY_ARCH="${SERENITY_ARCH:-i686}"
LLVM_VERSION="${LLVM_VERSION:-13.0.0}"

if [ "$SERENITY_TOOLCHAIN" = "Clang" ]; then
    TOOLCHAIN_DIR="$SERENITY_SOURCE_DIR"/Toolchain/Local/clang/
    $CP "$TOOLCHAIN_DIR"/lib/"$SERENITY_ARCH"-pc-serenity/* mnt/usr/lib
    mkdir -p mnt/usr/include/"$SERENITY_ARCH"-pc-serenity
    $CP -r "$TOOLCHAIN_DIR"/include/c++ mnt/usr/include
    $CP -r "$TOOLCHAIN_DIR"/include/"$SERENITY_ARCH"-pc-serenity/c++ mnt/usr/include/"$SERENITY_ARCH"-pc-serenity
elif [ "$SERENITY_ARCH" != "aarch64" ]; then
    $CP "$SERENITY_SOURCE_DIR"/Toolchain/Local/"$SERENITY_ARCH"/"$SERENITY_ARCH"-pc-serenity/lib/libgcc_s.so mnt/usr/lib
    $CP "$SERENITY_SOURCE_DIR"/Toolchain/Local/"$SERENITY_ARCH"/"$SERENITY_ARCH"-pc-serenity/lib/libstdc++.a mnt/usr/lib
    $CP -r "$SERENITY_SOURCE_DIR"/Toolchain/Local/"$SERENITY_ARCH"/"$SERENITY_ARCH"-pc-serenity/include/c++ mnt/usr/include
fi

# If umask was 027 or similar when the repo was cloned,
# file permissions in Base/ are too restrictive. Restore
# the permissions needed in the image.
chmod -R g+rX,o+rX "$SERENITY_SOURCE_DIR"/Base/* mnt/

chmod 660 mnt/etc/WindowServer.ini
chown $window_uid:$window_gid mnt/etc/WindowServer.ini
echo "/bin/sh" > mnt/etc/shells

if [ -f mnt/bin/su ]; then
    chown 0:$wheel_gid mnt/bin/su
    chmod 4750 mnt/bin/su
fi
if [ -f mnt/bin/passwd ]; then
    chown 0:$wheel_gid mnt/bin/passwd
    chmod 4755 mnt/bin/passwd
fi
if [ -f mnt/bin/ping ]; then
    chown 0:$wheel_gid mnt/bin/ping
    chmod 4755 mnt/bin/ping
fi
if [ -f mnt/bin/traceroute ]; then
    chown 0:$wheel_gid mnt/bin/traceroute
    chmod 4755 mnt/bin/traceroute
fi
if [ -f mnt/bin/keymap ]; then
    chown 0:$phys_gid mnt/bin/keymap
    chmod 4750 mnt/bin/keymap
fi
if [ -f mnt/bin/shutdown ]; then
    chown 0:$phys_gid mnt/bin/shutdown
    chmod 4750 mnt/bin/shutdown
fi
if [ -f mnt/bin/reboot ]; then
    chown 0:$phys_gid mnt/bin/reboot
    chmod 4750 mnt/bin/reboot
fi
if [ -f mnt/bin/pls ]; then
    chown 0:$wheel_gid mnt/bin/pls
    chmod 4750 mnt/bin/pls
fi
if [ -f mnt/bin/utmpupdate ]; then
    chown 0:$utmp_gid mnt/bin/utmpupdate
    chmod 2755 mnt/bin/utmpupdate
fi
if [ -f mnt/bin/timezone ]; then
    chown 0:$phys_gid mnt/bin/timezone
    chmod 4750 mnt/bin/timezone
fi
if [ -f mnt/usr/Tests/Kernel/TestMemoryDeviceMmap ]; then
    chown 0:0 mnt/usr/Tests/Kernel/TestMemoryDeviceMmap
    chmod 4755 mnt/usr/Tests/Kernel/TestMemoryDeviceMmap
fi
if [ -f mnt/usr/Tests/Kernel/TestProcFSWrite ]; then
    chown 0:0 mnt/usr/Tests/Kernel/TestProcFSWrite
    chmod 4755 mnt/usr/Tests/Kernel/TestProcFSWrite
fi

chmod 0400 mnt/res/kernel.map
chmod 0400 mnt/boot/Kernel
chmod 0400 mnt/boot/Kernel.debug
chmod 600 mnt/etc/shadow
chmod 755 mnt/res/devel/templates/*.postcreate
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
echo "done"

printf "setting up sysfs folder... "
mkdir -p mnt/sys
echo "done"

printf "writing version file... "
GIT_HASH=$( (git log --pretty=format:'%h' -n 1 | cut -c1-7) || true )
printf "[Version]\nMajor=1\nMinor=0\nGit=%s\n" "$GIT_HASH" > mnt/res/version.ini
echo "done"

printf "installing users... "
mkdir -p mnt/root
mkdir -p mnt/home/anon
mkdir -p mnt/home/anon/Desktop
mkdir -p mnt/home/anon/Downloads
mkdir -p mnt/home/nona
rm -fr mnt/home/anon/js-tests mnt/home/anon/web-tests mnt/home/anon/cpp-tests mnt/home/anon/wasm-tests
mkdir -p mnt/home/anon/cpp-tests/
cp "$SERENITY_SOURCE_DIR"/README.md mnt/home/anon/
cp -r "$SERENITY_SOURCE_DIR"/Userland/Libraries/LibJS/Tests mnt/home/anon/js-tests
cp -r "$SERENITY_SOURCE_DIR"/Userland/Libraries/LibWeb/Tests mnt/home/anon/web-tests
cp -r "$SERENITY_SOURCE_DIR"/Userland/DevTools/HackStudio/LanguageServers/Cpp/Tests mnt/home/anon/cpp-tests/comprehension
cp -r "$SERENITY_SOURCE_DIR"/Userland/Libraries/LibCpp/Tests/parser mnt/home/anon/cpp-tests/parser
cp -r "$SERENITY_SOURCE_DIR"/Userland/Libraries/LibCpp/Tests/preprocessor mnt/home/anon/cpp-tests/preprocessor
cp -r "$SERENITY_SOURCE_DIR"/Userland/Libraries/LibWasm/Tests mnt/home/anon/wasm-tests
cp -r "$SERENITY_SOURCE_DIR"/Userland/Libraries/LibJS/Tests/test-common.js mnt/home/anon/wasm-tests
cp -r "$SERENITY_SOURCE_DIR"/Userland/Applications/Spreadsheet/Tests mnt/home/anon/spreadsheet-tests

if [ -n "$SERENITY_COPY_SOURCE" ] ; then
  printf "\ncopying Serenity's source... "
  rm -fr mnt/home/anon/Source/serenity
  mkdir -p mnt/home/anon/Source/serenity
  git clone --depth=1 file://"$SERENITY_SOURCE_DIR" mnt/home/anon/Source/serenity
  rm -fr mnt/home/anon/Source/serenity/.git
fi

chmod 700 mnt/root
chmod 700 mnt/home/anon
chmod 700 mnt/home/nona
chown -R 100:100 mnt/home/anon
chown -R 200:200 mnt/home/nona
echo "done"

printf "adding some desktop icons... "
ln -sf /bin/Browser mnt/home/anon/Desktop/
ln -sf /bin/TextEditor mnt/home/anon/Desktop/Text\ Editor
ln -sf /bin/Help mnt/home/anon/Desktop/
ln -sf /home/anon mnt/home/anon/Desktop/Home
chown -R 100:100 mnt/home/anon/Desktop
echo "done"

printf "installing shortcuts... "
ln -sf Shell mnt/bin/sh
ln -sf test mnt/bin/[
echo "done"

printf "installing 'checksum' variants... "
ln -sf checksum mnt/bin/md5sum
ln -sf checksum mnt/bin/sha1sum
ln -sf checksum mnt/bin/sha256sum
ln -sf checksum mnt/bin/sha512sum
echo "done"

# Run local sync script, if it exists
if [ -f "${SERENITY_SOURCE_DIR}/sync-local.sh" ]; then
    sh "${SERENITY_SOURCE_DIR}/sync-local.sh"
fi
