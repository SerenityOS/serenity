## Name

mount - mount a filesystem

## Synopsis

```**sh
$ mount
# mount -a
# mount <source> <target> [-t fstype] [-o options]
```

## Description

If invoked without any arguments, `mount` prints a list of all currently mounted filesystems.

If invoked as `mount -a`, `mount` mounts all the filesystems configured in `/etc/fstab`. This
is normally done on system startup by [`SystemServer`(7)](../man7/SystemServer.md).

Otherwise, `mount` performs a single filesystem mount. Source, target, and fstype have the
same meaning as in the [`mount`(2)](../man2/mount.md) syscall (if not specified, fstype
defaults to `ext2`).

Options correspond to the mount flags, and should be specified as a comma-separated list of
flag names (lowercase and without the `MS_` prefix). Additionally, the name `defaults` is
accepted and ignored.

## Files

* `/etc/fstab` - read by `mount -a` on startup to find out which filesystems to mount.
* `/proc/df` - read by `mount` to get information about mounted filesystems.

## Examples

```sh
# mount devpts /dev/pts -t devpts -o noexec,nosuid
# mount /home/anon/myfile.txt /tmp/foo -o bind
```

## See also

* [`mount`(2)](../man2/mount.md)
