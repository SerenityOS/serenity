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

For Ext2FS, `source_fd` must refer to an open file descriptor to a file containing
the filesystem image. This may be a device file or any other seekable file. All
the other filesystems ignore the `source_fd` — you can even pass an invalid file
descriptor such as -1.

The following `flags` are supported:

* `MS_NODEV`: Disallow opening any devices from this file system.
* `MS_NOEXEC`: Disallow executing any executables from this file system.
* `MS_NOSUID`: Ignore set-user-id bits on executables from this file system.
* `MS_BIND`: Perform a bind-mount (see below).

These flags can be used as a security measure to limit the possible abuses of the newly
mounted file system.

### Bind mounts

If `MS_BIND` is specified in `flags`, `fs_type` is ignored and a bind mount is
performed instead. In this case, the file or directory specified by `source_fd`
is overlayed over `target` — the target appears to be replaced by a copy of the
source. This can be used as an alternative to symlinks or hardlinks.

## Errors

* `EFAULT`: The `fs_type` or `target` are invalid strings.
* `EPERM`: The current process does not have superuser privileges.
* `ENODEV`: The `fs_type` is unrecognized, the file descriptor to source is not found, or the source doesn't contain a valid filesystem image. Also, this error occurs if `fs_type` is valid, but the file descriptor from `source_fd` is not seekable.
* `EBADF`: If the `source_fd` is not valid, and either `fs_type` specifies a file-backed filesystem (and not a pseudo filesystem), or `MS_BIND` is specified in flags.

All of the usual path resolution errors may also occur.

## See also

* [`mount`(8)](../man8/mount.md)
