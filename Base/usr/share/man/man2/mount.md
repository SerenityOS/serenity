## Name

mount - mount a filesystem

## Synopsis

```**c++
#include <unistd.h>

int mount(int source_fd, const char* target, const char* fs_type, int flags);
```

## Description

`mount()` mounts a filesystem stored at `source_fd` by overlaying its contents
over `target`.

`fs_type` must be one of the following supported filesystems:

* `Ext2FS` (or `ext2`): The ext2 filesystem.
* `ProcFS` (or `proc`): The process pseudo-filesystem (normally mounted at `/proc`).
* `DevPtsFS` (or `devpts`): The pseudoterminal pseudo-filesystem (normally mounted at `/dev/pts`).
* `TmpFS` (or `tmp`): A non-persistent filesystem that stores all its data in RAM. An instance of this filesystem is normally mounted at `/tmp`.
* `Plan9FS` (or `9p`): A remote filesystem served over the 9P protocol.

For Ext2FS, `source_fd` must refer to an open file descriptor to a file
containing the filesystem image. This may be a device file or any other seekable
file. For Plan9FS, `source_fd` must refer to a socket or a device connected to a
9P server. All the other filesystems ignore the `source_fd` - you can even pass
an invalid file descriptor such as -1.

The following `flags` are supported:

* `MS_NODEV`: Disallow opening any devices from this file system.
* `MS_NOEXEC`: Disallow executing any executables from this file system.
* `MS_NOSUID`: Ignore set-user-id bits on executables from this file system.
* `MS_BIND`: Perform a bind-mount (see below).
* `MS_RDONLY`: Mount the filesystem read-only.
* `MS_REMOUNT`: Remount an already mounted filesystem (see below).

These flags can be used as a security measure to limit the possible abuses of the newly
mounted file system.

### Bind mounts

If `MS_BIND` is specified in `flags`, `fs_type` is ignored and a bind mount is
performed instead. In this case, the file or directory specified by `source_fd`
is overlaid over `target` — the target appears to be replaced by a copy of the
source. This can be used as an alternative to symlinks or hardlinks.

Each bind mount has its own set of flags, independent of the others or the
original file system. It is possible to bind-mount a file or directory over
itself, which may be useful for changing mount flags for a part of a filesystem.

### Remounting

If `MS_REMOUNT` is specified in `flags`, `source_fd` and `fs_type` are ignored,
and a remount is performed instead. `target` must point to an existing mount
point. The mount flags for that mount point are reset to `flags` (except the
`MS_REMOUNT` flag itself, which is stripped from the value).

Note that remounting a file system will only affect future operations with the
file system, not any already opened files. For example, if you open a directory
on a filesystem that's mounted with `MS_NODEV`, then remount the filesystem to
allow opening devices, attempts to open a devices relative to the directory file
descriptor (such as by using `openat()`) will still fail.

In particular, current working directory and root directory of any already
running processes behave the same way, and don't automatically "pick up" changes
in mount flags of the underlying file system. To "refresh" the working directory
to use the new mount flags after remounting a filesystem, a process can call
`chdir()` with the path to the same directory.

Similarly, to change the mount flags used by the root directory, a process can
call [`chroot_with_mount_flags`(2)](chroot.md), specifying a single slash (`/`)
as the path along with the desired flags. While is's possible to remount the
root filesystem using `MS_REMOUNT`, it would only have a noticeable effect if
the kernel was to launch more userspace processes directly, the way it does
launch the initial userspace process.

## Errors

* `EFAULT`: The `fs_type` or `target` are invalid strings.
* `EPERM`: The current process does not have superuser privileges.
* `ENODEV`: The `fs_type` is unrecognized, or the file descriptor to source is
  not found, or the source doesn't contain a valid filesystem image. Also, this
  error occurs if `fs_type` is valid and required to be seekable, but the file
  descriptor from `source_fd` is not seekable.
* `EBADF`: If the `source_fd` is not valid, and either `fs_type` specifies a
  file-backed filesystem (and not a pseudo filesystem), or `MS_BIND` is
  specified in flags.

All of the usual path resolution errors may also occur.

## See also

* [`mount`(8)](../man8/mount.md)
