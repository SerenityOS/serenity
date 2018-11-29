rm -vf _fs_contents.lock
rm -vf _fs_contents
cp -vp _fs_contents.stock _fs_contents
mkdir -vp mnt
mount -o loop _fs_contents mnt/
mkdir -vp mnt/dev
mknod mnt/dev/tty0 c 4 0
mknod mnt/dev/tty1 c 4 1
mknod mnt/dev/tty2 c 4 2
mknod mnt/dev/tty3 c 4 3
cp -R ../Base/* mnt/
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
cp -v ../Userland/ft mnt/bin/ft
cp -v ../Userland/ft2 mnt/bin/ft2
cp -v ../Userland/mm mnt/bin/mm
cp -v ../Userland/kill mnt/bin/kill
cp -v ../Userland/tty mnt/bin/tty
cp -v ../Userland/strsignal mnt/bin/strsignal
cp -v ../Userland/mkdir mnt/bin/mkdir
sh sync-local.sh
cp -v kernel.map mnt/
ln -s dir_a mnt/dir_cur
ln -s nowhere mnt/bad_link
umount mnt
sync
