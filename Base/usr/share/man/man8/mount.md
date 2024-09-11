## Name

mount - mount a filesystem

## Synopsis

```**sh
$ mount
# mount -a
# mount <source> <target> [-t fstype] [-o options]
```

## Description

If invoked without any arguments, `mount` prints a list of all currently mounted
filesystems.

If invoked as `mount -a`, `mount` mounts all the filesystems configured in
`/etc/fstab` and `/etc/fstab.d/*`. This is normally done on system startup by
[`SystemServer`(7)](help://man/7/SystemServer).

Otherwise, `mount` performs a single filesystem mount. Source should be a path
to a file containing the filesystem image. Target and fstype have the same
meaning as in the [`mount`(2)](help://man/2/mount) syscall (if not specified,
fstype defaults to `ext2`).

A special source value "none" is recognized, in which case
[`mount`(8)](help://man/8/mount) will not attempt to open the source as a file, and will
pass an invalid file descriptor to [`mount`(2)](help://man/2/mount). This is
useful for mounting pseudo filesystems.

Options correspond to the mount flags, and should be specified as a
comma-separated list of flag names (lowercase and without the `MS_` prefix).
Additionally, the name `defaults` is accepted and ignored.

## Files

-   `/etc/fstab` - read by `mount -a` on startup to find out which filesystems to mount.
-   `/etc/fstab.d` - directory with drop-in additions to the normal `fstab` file, also read by `mount -a`.
-   `/sys/kernel/df` - read by `mount` to get information about mounted filesystems.

## Examples

```sh
# mount devpts /dev/pts -t devpts -o noexec,nosuid
# mount /home/anon/myfile.txt /tmp/foo -o bind

# mount a regular file using a temporary loop device
$ mount /home/anon/myfilesystem.bin /mnt
```

## See also

-   [`mount`(2)](help://man/2/mount)
-   [`umount`(8)](help://man/8/umount)
