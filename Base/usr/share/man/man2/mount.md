## Name

mount - mount a filesystem

## Synopsis

```**c++
#include <unistd.h>

int mount(const char* source, const char* target, const char* fs_type, int flags);
```

## Description

`mount()` mounts a filesystem stored at `source` by overlaying its contents over `target`.

`fs_type` must be one of the following supported filesystems:

* `Ext2FS` (or `ext2`): The ext2 filesystem.
* `ProcFS` (or `proc`): The process pseudo-filesystem (normally mounted at `/proc`).
* `DevPtsFS` (or `devpts`): The pseudoterminal pseudo-filesystem (normally mounted at `/dev/pts`).
* `TmpFS` (or `tmp`): A non-persistent filesystem that stores all its data in RAM. An instance of this filesystem is normally mounted at `/tmp`.

For Ext2FS, `source` must be a path to a block device storing the filesystem contents. All
the other filesystems ignore the `source` argument (by convention, it should have the same
value as `fs_type`).

The following `flags` are supported:

* `MS_NODEV`: Disallow opening any devices from this file system.
* `MS_NOEXEC`: Disallow executing any executables from this file system.
* `MS_NOSUID`: Ignore set-user-id bits on executables from this file system.
* `MS_BIND`: Perform a bind-mount (see below).

These flags can be used as a security measure to limit the possible abuses of the newly
mounted file system.

### Bind mounts

If `MS_BIND` is specified in `flags`, `fs_type` is ignored and a bind mount is performed
instead. In this case `source` is treated as a path to a file or directory whose contents
are overlayed over `target`. This can be used  as an alternative to symlinks or hardlinks.

## Errors

* `EPERM`: The current process does not have superuser privileges.
* `ENODEV`: The `fs_type` is unrecognized, or the device is not found, or the device doesn't contain a valid filesystem image.

All of the usual path resolution errors may also occur.

## See also

* [`mount`(8)](../man8/mount.md)
