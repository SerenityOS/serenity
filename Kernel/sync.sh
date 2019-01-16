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
mknod mnt/dev/psaux c 10 1
mknod mnt/dev/ptmx c 5 2
mknod mnt/dev/ptm0 c 10 0
mknod mnt/dev/ptm1 c 10 1
mknod mnt/dev/ptm2 c 10 2
mknod mnt/dev/ptm3 c 10 3
mknod mnt/dev/pts0 c 11 0
mknod mnt/dev/pts1 c 11 1
mknod mnt/dev/pts2 c 11 2
mknod mnt/dev/pts3 c 11 3
mknod mnt/dev/gui_events c 66 1
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
cp -v ../Userland/mm mnt/bin/mm
cp -v ../Userland/kill mnt/bin/kill
cp -v ../Userland/tty mnt/bin/tty
cp -v ../Userland/mkdir mnt/bin/mkdir
cp -v ../Userland/touch mnt/bin/touch
cp -v ../Userland/sync mnt/bin/sync
cp -v ../Userland/more mnt/bin/more
cp -v ../Userland/guitest mnt/bin/guitest
cp -v ../Terminal/Terminal mnt/bin/Terminal
sh sync-local.sh
cp -v kernel.map mnt/
ln -s dir_a mnt/dir_cur
ln -s nowhere mnt/bad_link
umount mnt
sync
