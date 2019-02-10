if [ $(id -u) != 0 ]; then
    echo "This needs to be run as root"
    exit
fi
rm -vf _fs_contents.lock
rm -vf _fs_contents
dd if=/dev/zero of=_fs_contents bs=1M count=12
mke2fs _fs_contents
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
mknod mnt/dev/tty0 c 4 0
mknod mnt/dev/tty1 c 4 1
mknod mnt/dev/tty2 c 4 2
mknod mnt/dev/tty3 c 4 3
mknod mnt/dev/keyboard c 85 1
mknod mnt/dev/psaux c 10 1
mknod mnt/dev/ptmx c 5 2
mknod mnt/dev/gui_events c 66 1
ln -s /proc/self/fd/0 mnt/dev/stdin
ln -s /proc/self/fd/1 mnt/dev/stdout
ln -s /proc/self/fd/2 mnt/dev/stderr
cp -vR ../Base/* mnt/
chown -vR 100:100 mnt/home/anon
cp -v ../Userland/sh mnt/bin/sh
cp -v ../Userland/id mnt/bin/id
cp -v ../Userland/ps mnt/bin/ps
cp -v ../Userland/ls mnt/bin/ls
cp -v ../Userland/fgrep mnt/bin/fgrep
cp -v ../Userland/sleep mnt/bin/sleep
cp -v ../Userland/date mnt/bin/date
cp -v ../Userland/true mnt/bin/true
cp -v ../Userland/false mnt/bin/false
cp -v ../Userland/hostname mnt/bin/hostname
cp -v ../Userland/cat mnt/bin/cat
cp -v ../Userland/uname mnt/bin/uname
cp -v ../Userland/clear mnt/bin/clear
cp -v ../Userland/tst mnt/bin/tst
cp -v ../Userland/mm mnt/bin/mm
cp -v ../Userland/kill mnt/bin/kill
cp -v ../Userland/tty mnt/bin/tty
cp -v ../Userland/mkdir mnt/bin/mkdir
cp -v ../Userland/touch mnt/bin/touch
cp -v ../Userland/sync mnt/bin/sync
cp -v ../Userland/more mnt/bin/more
cp -v ../Userland/rm mnt/bin/rm
cp -v ../Userland/rmdir mnt/bin/rmdir
cp -v ../Userland/cp mnt/bin/cp
cp -v ../Userland/guitest mnt/bin/guitest
cp -v ../Userland/guitest2 mnt/bin/guitest2
cp -v ../Userland/sysctl mnt/bin/sysctl
cp -v ../Userland/pape mnt/bin/pape
cp -v ../Userland/dmesg mnt/bin/dmesg
cp -v ../Userland/chmod mnt/bin/chmod
cp -v ../Userland/top mnt/bin/top
cp -v ../Applications/Terminal/Terminal mnt/bin/Terminal
cp -v ../Applications/FontEditor/FontEditor mnt/bin/FontEditor
cp -v ../Applications/Launcher/Launcher mnt/bin/Launcher
cp -v ../Applications/Clock/Clock mnt/bin/Clock
cp -v ../Applications/FileManager/FileManager mnt/bin/FileManager
cp -v kernel.map mnt/
sh sync-local.sh
umount mnt
sync
