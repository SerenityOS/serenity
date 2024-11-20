#!/bin/sh

set -e

wheel_gid=1
phys_gid=3
utmp_gid=5
window_uid=13
window_gid=13

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
    rsync -aH --chown=0:0 --inplace --update "$SERENITY_SOURCE_DIR"/Base/ mnt/
    rsync -aH --chown=0:0 --exclude="/usr/include" --inplace --update Root/ mnt/
    rsync -aHL --chown=0:0 --inplace --update Root/usr/include/ mnt/usr/include/
else
    rsync -aH --inplace --update "$SERENITY_SOURCE_DIR"/Base/ mnt/
    rsync -aH --inplace --exclude="/usr/include" --update Root/ mnt/
    rsync -aHL --inplace --update Root/usr/include/ mnt/usr/include/
    chown -R 0:0 mnt/
fi

SERENITY_ARCH="${SERENITY_ARCH:-x86_64}"

if [ "$SERENITY_TOOLCHAIN" = "Clang" ]; then
    TOOLCHAIN_DIR="$SERENITY_SOURCE_DIR"/Toolchain/Local/clang/
    rsync -aH --update -t "$TOOLCHAIN_DIR"/lib/"$SERENITY_ARCH"-pc-serenity/* mnt/usr/lib
    mkdir -p mnt/usr/include/"$SERENITY_ARCH"-pc-serenity
    rsync -aH --update -t -r "$TOOLCHAIN_DIR"/include/c++ mnt/usr/include
    rsync -aH --update -t -r "$TOOLCHAIN_DIR"/include/"$SERENITY_ARCH"-pc-serenity/c++ mnt/usr/include/"$SERENITY_ARCH"-pc-serenity
else
    rsync -aH --update -t -r "$SERENITY_SOURCE_DIR"/Toolchain/Local/"$SERENITY_ARCH"/"$SERENITY_ARCH"-pc-serenity/lib/* mnt/usr/lib
    rsync -aH --update -t -r "$SERENITY_SOURCE_DIR"/Toolchain/Local/"$SERENITY_ARCH"/"$SERENITY_ARCH"-pc-serenity/include/c++ mnt/usr/include
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
if [ -f mnt/bin/Escalator ]; then
    chown 0:$wheel_gid mnt/bin/Escalator
    chmod 4750 mnt/bin/Escalator
fi
if [ -f mnt/bin/utmpupdate ]; then
    chown 0:$utmp_gid mnt/bin/utmpupdate
    chmod 2755 mnt/bin/utmpupdate
fi
if [ -f mnt/bin/timezone ]; then
    chown 0:$phys_gid mnt/bin/timezone
    chmod 4750 mnt/bin/timezone
fi
if [ -f mnt/usr/Tests/Kernel/TestExt2FS ]; then
    chown 0:0 mnt/usr/Tests/Kernel/TestExt2FS
    chmod 4755 mnt/usr/Tests/Kernel/TestExt2FS
fi
if [ -f mnt/usr/Tests/Kernel/TestMemoryDeviceMmap ]; then
    chown 0:0 mnt/usr/Tests/Kernel/TestMemoryDeviceMmap
    chmod 4755 mnt/usr/Tests/Kernel/TestMemoryDeviceMmap
fi
if [ -f mnt/usr/Tests/Kernel/TestProcFSWrite ]; then
    chown 0:0 mnt/usr/Tests/Kernel/TestProcFSWrite
    chmod 4755 mnt/usr/Tests/Kernel/TestProcFSWrite
fi
if [ -f mnt/usr/Tests/Kernel/TestLoopDevice ]; then
    chown 0:0 mnt/usr/Tests/Kernel/TestLoopDevice
    chmod 4755 mnt/usr/Tests/Kernel/TestLoopDevice
fi

if [ -f mnt/res/kernel.map ]; then
    chmod 0400 mnt/res/kernel.map
fi

if [ -f mnt/boot/Kernel ]; then
    chmod 0400 mnt/boot/Kernel
fi

if [ -f mnt/boot/Kernel.debug ]; then
    chmod 0400 mnt/boot/Kernel.debug
fi

if [ -f mnt/bin/network-settings ]; then
    chown 0:0 mnt/bin/network-settings
    chmod 500 mnt/bin/network-settings
fi

chmod 600 mnt/etc/shadow
chmod 755 mnt/res/devel/templates/*.postcreate
echo "done"

printf "creating initial filesystem structure... "
for dir in bin etc proc mnt tmp boot www var/run usr/local usr/Ports usr/bin; do
    mkdir -p mnt/$dir
done
chmod 700 mnt/boot
chmod 1777 mnt/tmp
echo "done"

printf "creating utmp file... "
echo "{}" > mnt/var/run/utmp
chown 0:$utmp_gid mnt/var/run/utmp
chmod 664 mnt/var/run/utmp
echo "done"

printf "setting up device nodes folder... "
mkdir -p mnt/dev
echo "done"

printf "setting up sysfs folder... "
mkdir -p mnt/sys
echo "done"

printf "installing users... "
mkdir -p mnt/root
mkdir -p mnt/home/anon
mkdir -p mnt/home/anon/Desktop
mkdir -p mnt/home/anon/Downloads
mkdir -p mnt/home/anon/Music
mkdir -p mnt/home/anon/Pictures
mkdir -p mnt/home/nona
# FIXME: Handle these test copies using CMake install rules
rm -fr mnt/home/anon/Tests/js-tests mnt/home/anon/Tests/cpp-tests
mkdir -p mnt/home/anon/Tests/cpp-tests/
cp "$SERENITY_SOURCE_DIR"/README.md mnt/home/anon/
cp -r "$SERENITY_SOURCE_DIR"/Userland/Libraries/LibJS/Tests mnt/home/anon/Tests/js-tests
cp -r "$SERENITY_SOURCE_DIR"/Userland/Libraries/LibCodeComprehension/Cpp/Tests mnt/home/anon/Tests/cpp-tests/comprehension
cp -r "$SERENITY_SOURCE_DIR"/Userland/Libraries/LibCpp/Tests/parser mnt/home/anon/Tests/cpp-tests/parser
cp -r "$SERENITY_SOURCE_DIR"/Userland/Libraries/LibCpp/Tests/preprocessor mnt/home/anon/Tests/cpp-tests/preprocessor
cp -r "$SERENITY_SOURCE_DIR"/Userland/Libraries/LibJS/Tests/test-common.js mnt/home/anon/Tests/wasm-tests
cp -r "$SERENITY_SOURCE_DIR"/Userland/Applications/Spreadsheet/Tests mnt/home/anon/Tests/spreadsheet-tests

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
chown -R 200:100 mnt/home/nona
echo "done"

printf "adding some desktop icons... "
ln -sf /bin/Browser mnt/home/anon/Desktop/
ln -sf /bin/TextEditor mnt/home/anon/Desktop/Text\ Editor
ln -sf /bin/Help mnt/home/anon/Desktop/
ln -sf /home/anon mnt/home/anon/Desktop/Home
chown -R 100:100 mnt/home/anon/Desktop
echo "done"

printf "installing shortcuts... "
ln -sf /bin/PackageManager mnt/bin/pkg
ln -sf /bin/RunContainer mnt/bin/runc
ln -sf Shell mnt/bin/sh
ln -sf test mnt/bin/[
ln -sf less mnt/bin/more
ln -sf /bin/env mnt/usr/bin/env
ln -sf /bin/init mnt/init
echo "done"

printf "installing 'checksum' variants... "
ln -sf checksum mnt/bin/b2sum
ln -sf checksum mnt/bin/md5sum
ln -sf checksum mnt/bin/sha1sum
ln -sf checksum mnt/bin/sha256sum
ln -sf checksum mnt/bin/sha512sum
echo "done"

# Run local sync script, if it exists
if [ -f "${SERENITY_SOURCE_DIR}/sync-local.sh" ]; then
    sh "${SERENITY_SOURCE_DIR}/sync-local.sh"
fi
